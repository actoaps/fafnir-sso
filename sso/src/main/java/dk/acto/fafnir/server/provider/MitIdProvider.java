package dk.acto.fafnir.server.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.oauth.OAuth20Service;
import dk.acto.fafnir.api.model.FafnirUser;
import dk.acto.fafnir.api.model.UserData;
import dk.acto.fafnir.server.FailureReason;
import dk.acto.fafnir.server.TokenFactory;
import dk.acto.fafnir.server.model.CallbackResult;
import dk.acto.fafnir.server.model.conf.MitIdConf;
import dk.acto.fafnir.server.model.conf.TestConf;
import dk.acto.fafnir.server.provider.credentials.TokenCredentials;
import io.vavr.control.Option;
import io.vavr.control.Try;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@AllArgsConstructor
@ConditionalOnBean(name = "mitIdOauth")
public class MitIdProvider implements RedirectingAuthenticationProvider<TokenCredentials> {
    @Qualifier("mitIdOauth")
    private final OAuth20Service mitIdOauth;
    private final MitIdConf mitIdConf;
    private final TestConf testConf;
    private final TokenFactory tokenFactory;
    private final ObjectMapper objectMapper;

    @Override
    public String authenticate() {
        return mitIdOauth.getAuthorizationUrl();
    }

    @Override
    public CallbackResult callback(TokenCredentials data) {
        var code = data.getCode();
        final OAuth2AccessToken token = Option.of(code)
                .toTry()
                .mapTry(mitIdOauth::getAccessToken)
                .onFailure(x -> log.error("Authentication failed", x))
                .getOrNull();

        if (token == null) {
            return CallbackResult.failure(FailureReason.AUTHENTICATION_FAILED);
        }

        var userInfo = getUserInfo(token.getAccessToken());

        if (userInfo == null) {
            return CallbackResult.failure(FailureReason.AUTHENTICATION_FAILED);
        }

        String jwt = tokenFactory.generateToken(FafnirUser.builder()
                .data(UserData.builder()
                        .subject(userInfo.getLeft())
                        .provider("mitid")
                        .name(userInfo.getRight())
                        .build())
                .build());
        return CallbackResult.success(jwt);
    }

    @Override
    public boolean supportsOrganisationUrls() {
        return false;
    }

    @Override
    public String entryPoint() {
        return "mitid";
    }

    private Pair<String, String> getUserInfo(String token) {
        var url = mitIdConf.getAuthorityUrl() + "/connect/userinfo";
        var rest = new RestTemplate();
        var headers = new HttpHeaders();
        headers.setBearerAuth(token);

        var response = rest.exchange(url, HttpMethod.GET, new HttpEntity<String>(headers), String.class);

        return Try.of(() -> objectMapper.readTree(response.getBody()))
                .map(x -> Pair.of(
                        x.get("sub").asText(),
                        x.get(testConf.isEnabled() ? "mitid_demo.full_name" : "mitid.full_name").asText()))
                .getOrNull();
    }
}
