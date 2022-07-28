package dk.acto.fafnir.sso.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.oauth.OAuth20Service;
import dk.acto.fafnir.api.model.*;
import dk.acto.fafnir.api.provider.RedirectingAuthenticationProvider;
import dk.acto.fafnir.api.provider.metadata.MetadataProvider;
import dk.acto.fafnir.api.service.AdministrationService;
import dk.acto.fafnir.sso.provider.credentials.TokenCredentials;
import dk.acto.fafnir.sso.util.TokenFactory;
import io.vavr.control.Option;
import io.vavr.control.Try;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

@Slf4j
@AllArgsConstructor
public class MitIdProvider implements RedirectingAuthenticationProvider<TokenCredentials> {
    private final OAuth20Service mitIdOauth;
    private final TokenFactory tokenFactory;
    private final ObjectMapper objectMapper;
    private final AdministrationService administrationService;
    private final String authorityUrl;
    private final boolean isTest;

    @Override
    public String authenticate() {
        return mitIdOauth.getAuthorizationUrl();
    }

    @Override
    public AuthenticationResult callback(TokenCredentials data) {
        var code = data.getCode();
        final OAuth2AccessToken token = Option.of(code)
                .toTry()
                .mapTry(mitIdOauth::getAccessToken)
                .onFailure(x -> log.error("Authentication failed", x))
                .getOrNull();

        if (token == null) {
            return AuthenticationResult.failure(FailureReason.AUTHENTICATION_FAILED);
        }

        var userInfo = getUserInfo(token.getAccessToken());

        if (userInfo == null) {
            return AuthenticationResult.failure(FailureReason.AUTHENTICATION_FAILED);
        }
        var subjectActual = UserData.builder()
                .subject(userInfo.getLeft())
                .name(userInfo.getRight())
                .build();
        var orgActual = administrationService.readOrganisation(test -> test.getProviderId().equals(getMetaData().getProviderId()));
        var claimsActual = ClaimData.empty();

        String jwt = tokenFactory.generateToken(subjectActual, orgActual, claimsActual, getMetaData());

        return AuthenticationResult.success(jwt);
    }

    @Override
    public ProviderMetaData getMetaData() {
        return MetadataProvider.MIT_ID;
    }

    private Pair<String, String> getUserInfo(String token) {
        var url = authorityUrl + "/connect/userinfo";
        var rest = new RestTemplate();
        var headers = new HttpHeaders();
        headers.setBearerAuth(token);

        var response = rest.exchange(url, HttpMethod.GET, new HttpEntity<String>(headers), String.class);

        return Try.of(() -> objectMapper.readTree(response.getBody()))
                .map(x -> Pair.of(
                        x.get("sub").asText(),
                        x.get(isTest ? "mitid_demo.full_name" : "mitid.full_name").asText()))
                .getOrNull();
    }
}
