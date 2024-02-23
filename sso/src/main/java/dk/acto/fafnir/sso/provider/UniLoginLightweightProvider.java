package dk.acto.fafnir.sso.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.scribejava.core.oauth.OAuth20Service;
import dk.acto.fafnir.api.exception.ProviderAttributeMissing;
import dk.acto.fafnir.api.model.*;
import dk.acto.fafnir.api.provider.RedirectingAuthenticationProvider;
import dk.acto.fafnir.api.provider.metadata.MetadataProvider;
import dk.acto.fafnir.sso.model.conf.ProviderConf;
import dk.acto.fafnir.sso.provider.credentials.TokenCredentials;
import dk.acto.fafnir.sso.provider.unilogin.AccessToken;
import dk.acto.fafnir.sso.provider.unilogin.IntrospectionToken;
import dk.acto.fafnir.sso.provider.unilogin.UniloginTokenCredentials;
import dk.acto.fafnir.sso.util.PkceUtil;
import dk.acto.fafnir.sso.util.TokenFactory;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@AllArgsConstructor
public class UniLoginLightweightProvider implements RedirectingAuthenticationProvider<UniloginTokenCredentials> {
    private final OAuth20Service uniloginOauth;
    private final TokenFactory tokenFactory;
    private final ProviderConf providerConf;

    @Override
    public String authenticate() throws NoSuchAlgorithmException {
        String responseType = "response_type=" + URLEncoder.encode("code");
        String client = "&client_id=" + URLEncoder.encode("http://localhost:8080/");
        String redirect = "&redirect_uri=" + URLEncoder.encode("http://localhost:8080/unilogin-lightweight/callback");
        String codeChallengeMethod = "&code_challenge_method=" + URLEncoder.encode("S256");
        String codeVerifier = PkceUtil.generateCodeVerifier();
        String codeChallenge = "&code_challenge=" + URLEncoder.encode(PkceUtil.generateCodeChallenge(codeVerifier));
        String nonce = "&nonce=" + URLEncoder.encode(new SecureRandom().ints(16, 0, 256)
            .mapToObj(i -> String.format("%02x", i))
            .collect(Collectors.joining()));
        String state = "&state=" + URLEncoder.encode(codeVerifier);
        String scope = "&scope=" + URLEncoder.encode("openid");
        String responseMode = "&response_mode=" + URLEncoder.encode("form_post");
        return "https://et-broker.unilogin.dk/auth/realms/broker/protocol/openid-connect" + "/auth?" + responseType + client + redirect + codeChallengeMethod + codeChallenge + nonce + state + scope + responseMode;
    }

    @Override
    public AuthenticationResult callback(UniloginTokenCredentials data) throws IOException {
        var UL_CLIENT_ID = System.getenv("UL_CLIENT_ID");
        var UL_SECRET = System.getenv("UL_SECRET");
        var UL_REDIRECT_URL = System.getenv("UL_REDIRECT_URL");
        var CODE_VERIFIER = data.getState();
        var OID_BASE_URL = "https://et-broker.unilogin.dk/auth/realms/broker/protocol/openid-connect/";


        var accessCode = data.getCode();
        AccessToken accessToken;

        accessToken = getAccessToken(accessCode, UL_CLIENT_ID, UL_SECRET, UL_REDIRECT_URL, CODE_VERIFIER, OID_BASE_URL);

        IntrospectionToken intro;

        intro = getIntrospectToken(accessToken.getAccess_token(),UL_CLIENT_ID,UL_SECRET,OID_BASE_URL);


        if (intro == null) {
        return AuthenticationResult.failure(FailureReason.AUTHENTICATION_FAILED);
    }

        var subject = Optional.ofNullable(intro.getSub())
            .map(providerConf::applySubjectRules)
            .orElseThrow(ProviderAttributeMissing::new);

        var displayName = Optional.of("")
            .orElseThrow(ProviderAttributeMissing::new);

        var subjectActual = UserData.builder()
            .subject(subject)
            .name(displayName)
            .build();
        var orgActual = OrganisationData.DEFAULT;
        var claimsActual = ClaimData.empty();

        var jwt = tokenFactory.generateToken(subjectActual, orgActual, claimsActual, getMetaData());

        return AuthenticationResult.success(jwt);
    }

    private AccessToken getAccessToken(String code, String clientId, String clientSecret, String redirectUri, String codeVerifier, String oidcBaseUrl) throws IOException {
        List<NameValuePair> nvps = new ArrayList<>();
        nvps.add(new BasicNameValuePair("grant_type", "authorization_code"));
        nvps.add(new BasicNameValuePair("code", code));
        nvps.add(new BasicNameValuePair("client_id", clientId));
        nvps.add(new BasicNameValuePair("client_secret", clientSecret));
        nvps.add(new BasicNameValuePair("redirect_uri", redirectUri));
        nvps.add(new BasicNameValuePair("code_verifier", codeVerifier));
        HttpResponse response = httpPostRequest(oidcBaseUrl + "/token", nvps);

        return getObjectMapper().readValue(response.getEntity().getContent(), AccessToken.class);
    }

    private IntrospectionToken getIntrospectToken(String accesstoken,String clientId,String clientSecret, String oidcBaseUrl) throws IOException {
        List<NameValuePair> nvps = new ArrayList<>();
        nvps.add(new BasicNameValuePair("token", accesstoken));
        nvps.add(new BasicNameValuePair("client_id", clientId));
        nvps.add(new BasicNameValuePair("client_secret", clientSecret));
        HttpResponse response = httpPostRequest(oidcBaseUrl + "/token/introspect", nvps);

        return getObjectMapper().readValue(response.getEntity().getContent(), IntrospectionToken.class);
    }

    private HttpResponse httpPostRequest(String uri, List<NameValuePair> nvps) throws IOException {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost();
        httpPost.setURI(URI.create(uri));
        httpPost.setEntity(new UrlEncodedFormEntity(nvps));
        HttpResponse response = httpClient.execute(httpPost);
        return response;
    }

    ObjectMapper getObjectMapper() {
        return new ObjectMapper();
    }

    @Override
    public ProviderMetaData getMetaData() {
        return MetadataProvider.UNILOGIN;
    }
}
