package dk.acto.fafnir.sso.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import dk.acto.fafnir.api.model.*;
import dk.acto.fafnir.api.model.conf.FafnirConf;
import dk.acto.fafnir.api.provider.metadata.MetadataProvider;
import dk.acto.fafnir.sso.model.conf.ProviderConf;
import dk.acto.fafnir.sso.provider.unilogin.*;
import dk.acto.fafnir.sso.util.PkceUtil;
import dk.acto.fafnir.sso.util.TokenFactory;
import https.unilogin.Institutionstilknytning;
import https.wsibruger_unilogin_dk.ws.WsiBruger;
import https.wsiinst_unilogin_dk.ws.WsiInst;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;

import javax.xml.namespace.QName;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@AllArgsConstructor
public class UniLoginProvider {
    private final FafnirConf fafnirConf;
    private final TokenFactory tokenFactory;
    private final ProviderConf providerConf;
    private final UniLoginHelper uniloginHelper;

    public String authenticate(HttpSession session) throws NoSuchAlgorithmException {
        var codeVerifier = PkceUtil.generateCodeVerifier();
        session.setAttribute("codeVerifier", codeVerifier);

        var responseType = "response_type=" + URLEncoder.encode("code");
        var client = "&client_id=" + URLEncoder.encode(System.getenv("UL_CLIENT_ID"));
        var redirect = "&redirect_uri=" + URLEncoder.encode(System.getenv("FAFNIR_URL") + "/unilogin/callback");
        var codeChallengeMethod = "&code_challenge_method=" + URLEncoder.encode("S256");
        var codeChallenge = "&code_challenge=" + URLEncoder.encode(PkceUtil.generateCodeChallenge(codeVerifier));
        var nonce = "&nonce=" + URLEncoder.encode(new SecureRandom().ints(16, 0, 256)
            .mapToObj(i -> String.format("%02x", i))
            .collect(Collectors.joining()));
        var state = "&state=" + URLEncoder.encode(new SecureRandom().ints(16, 0, 256)
            .mapToObj(i -> String.format("%02x", i))
            .collect(Collectors.joining()));
        var scope = "&scope=" + URLEncoder.encode("openid");
        var responseMode = "&response_mode=" + URLEncoder.encode("form_post");
        return "https://broker.unilogin.dk/auth/realms/broker/protocol/openid-connect" + "/auth?" + responseType + client + redirect + codeChallengeMethod + codeChallenge + nonce + state + scope + responseMode;
    }

    public AuthenticationResult callback(UniloginTokenCredentials data, HttpSession session) throws IOException {
        var UL_CLIENT_ID = System.getenv("UL_CLIENT_ID");
        var UL_SECRET = System.getenv("UL_SECRET");
        var UL_REDIRECT_URL = System.getenv("FAFNIR_URL") + "/unilogin/callback";
        var OID_BASE_URL = "https://broker.unilogin.dk/auth/realms/broker/protocol/openid-connect/";

        var CODE_VERIFIER = (String) session.getAttribute("codeVerifier");


        var accessCode = data.getCode();
        AccessToken accessToken;

        accessToken = getAccessToken(accessCode, UL_CLIENT_ID, UL_SECRET, UL_REDIRECT_URL, CODE_VERIFIER, OID_BASE_URL);

        IntrospectionToken intro;

        intro = getIntrospectToken(accessToken.getAccess_token(), UL_CLIENT_ID, UL_SECRET, OID_BASE_URL);


        if (intro == null) {
            return AuthenticationResult.failure(FailureReason.AUTHENTICATION_FAILED);
        }

        var userId = intro.getUniid();

        var institutions = getInstitutionList(userId);
        if (institutions.isEmpty()) {
            return AuthenticationResult.failure(FailureReason.CONNECTION_FAILED);
        } else if (institutions.size() == 1) {
            return callbackWithInstitution(userId, institutions.get(0).getId());
        } else {
            String chooseInstitutionUrl = uniloginHelper.getChooseInstitutionUrl(userId);
            return AuthenticationResult.redirect(chooseInstitutionUrl);
        }
    }


    public List<Institution> getInstitutionList(String userId) {
        var wsdlURL = getClass().getClassLoader().getResource("wsdl/wsibruger_v6.wsdl");
        var SERVICE_NAME = new QName("https://wsibruger.unilogin.dk/ws", "WsiBruger");
        var wsiBruger = new WsiBruger(wsdlURL, SERVICE_NAME);
        var wsiBrugerPortType = wsiBruger.getWsiBrugerPort();
        List<Institutionstilknytning> institutionstilknytninger;


        var wsiURL = getClass().getClassLoader().getResource("wsdl/wsiinst_v5.wsdl");
        var SERVICE = new QName("https://wsiinst.unilogin.dk/ws", "WsiInst");
        var wsiInst = new WsiInst(wsiURL, SERVICE);
        var wsiInstPortType = wsiInst.getWsiInstPort();
        try {
            institutionstilknytninger = wsiBrugerPortType.hentBrugersInstitutionstilknytninger(uniloginHelper.getWsUsername(), uniloginHelper.getWsPassword(), userId);
            return institutionstilknytninger.stream().map((institutionstilknytning -> {
                var instName = "";
                try {
                    var inst = wsiInstPortType.hentInstitution(uniloginHelper.getWsUsername(), uniloginHelper.getWsPassword(), institutionstilknytning.getInstnr());
                    instName = inst.getInstnavn();
                } catch (https.wsiinst_unilogin_dk.ws.AuthentificationFault authentificationFault) {
                    log.error(authentificationFault.getMessage(), authentificationFault);
                }
                var roleNames = toUserRoles(institutionstilknytninger).stream()
                    .map(UserRole::toString)
                    .collect(Collectors.toList());
                return Institution.builder()
                    .name(instName)
                    .id(institutionstilknytning.getInstnr())
                    .roles(roleNames)
                    .build();
            })).distinct().collect(Collectors.toList());
        } catch (https.wsibruger_unilogin_dk.ws.AuthentificationFault authentificationFault) {
            log.error(authentificationFault.getMessage(), authentificationFault);
        }
        return Collections.emptyList();
    }


    private Set<UserRole> toUserRoles(java.util.List<https.unilogin.Institutionstilknytning> institutionstilknytninger) {
        Set<UserRole> roles = new HashSet<>();
        for (var institutionstilknytning : institutionstilknytninger) {
            var ansat = institutionstilknytning.getAnsat();
            var ekstern = institutionstilknytning.getEkstern();
            var elev = institutionstilknytning.getElev();
            if (ansat != null) {
                ansat.getRolle()
                    .forEach(ansatrolle -> UserRole.builder().name(ansatrolle.name()).type("EMPLOYEE").build());
            }
            if (ekstern != null) {
                roles.add(UserRole.builder()
                    .name(ekstern.getRolle().name())
                    .type("EMP_EXTERNAL")
                    .build());
            }
            if (elev != null) {
                roles.add(UserRole.builder()
                    .name(elev.getRolle().name())
                    .type("PUPIL")
                    .build());
            }
        }
        return roles;
    }

    public String getFailureUrl(FailureReason reason) {
        return fafnirConf.getFailureRedirect() + "#" + reason.getErrorCode();
    }


    public AuthenticationResult callbackWithInstitution(String userId, String institutionId) {
        var name = getUserFullNameFromId(userId);
        final var orgName = getInstitutionFromId(institutionId)
            .map(Institution::getName)
            .orElseThrow(() -> new RuntimeException("No institution"));

        var roles = this.getUserRoles(institutionId, userId);

        var subjectActual = UserData.builder()
            .subject(userId)
            .name(name)
            .build();
        var orgActual = OrganisationData.builder()
            .organisationId(institutionId)
            .organisationName(orgName)
            .build();
        var claimsActual = ClaimData.builder()
            .claims(roles.stream()
                .map(UserRole::toString)
                .toArray(String[]::new))
            .build();

        var jwt = tokenFactory.generateToken(subjectActual, orgActual, claimsActual, ProviderMetaData.builder()
            .providerName("UniLogin")
            .providerId("unilogin")
            .organisationSupport(OrganisationSupport.NATIVE)
            .inputs(List.of())
            .build());
        return AuthenticationResult.success(jwt);

    }

    private Optional<Institution> getInstitutionFromId(String institutionId) {
        var wsiURL = getClass().getClassLoader().getResource("wsdl/wsiinst_v5.wsdl");
        var SERVICE = new QName("https://wsiinst.unilogin.dk/ws", "WsiInst");
        var wsiInst = new WsiInst(wsiURL, SERVICE);
        var wsiInstPortType = wsiInst.getWsiInstPort();
        try {
            var inst = wsiInstPortType.hentInstitution(uniloginHelper.getWsUsername(), uniloginHelper.getWsPassword(), institutionId);
            return Optional.of(Institution.builder()
                .id(inst.getInstnr())
                .name(inst.getInstnavn())
                .build());
        } catch (https.wsiinst_unilogin_dk.ws.AuthentificationFault authentificationFault) {
            log.error(authentificationFault.getMessage(), authentificationFault);
            return Optional.empty();
        }
    }

    private Set<UserRole> getUserRoles(String institutionId, String userId) {
        var wsdlURL = getClass().getClassLoader().getResource("wsdl/wsibruger_v6.wsdl");
        var SERVICE_NAME = new QName("https://wsibruger.unilogin.dk/ws", "WsiBruger");
        var wsiBruger = new WsiBruger(wsdlURL, SERVICE_NAME);
        var wsiBrugerPortType = wsiBruger.getWsiBrugerPort();
        try {
            var institutionstilknytninger = wsiBrugerPortType.hentBrugersInstitutionstilknytninger(uniloginHelper.getWsUsername(), uniloginHelper.getWsPassword(), userId);
            institutionstilknytninger = institutionstilknytninger.stream()
                .filter(til -> institutionId.equals(til.getInstnr()))
                .collect(Collectors.toList());
            return toUserRoles(institutionstilknytninger);
        } catch (https.wsibruger_unilogin_dk.ws.AuthentificationFault authentificationFault) {
            log.error(authentificationFault.getMessage(), authentificationFault);
            return Collections.emptySet();
        }
    }

    private String getUserFullNameFromId(String userId) {
        return userId;
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

    private IntrospectionToken getIntrospectToken(String accesstoken, String clientId, String clientSecret, String oidcBaseUrl) throws IOException {
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

    public ProviderMetaData getMetaData() {
        return MetadataProvider.UNILOGIN;
    }
}
