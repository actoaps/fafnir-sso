package dk.acto.fafnir.server.providers;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.oauth.OAuth20Service;
import dk.acto.fafnir.api.model.FafnirUser;
import dk.acto.fafnir.server.FailureReason;
import dk.acto.fafnir.server.TokenFactory;
import dk.acto.fafnir.server.model.CallbackResult;
import dk.acto.fafnir.server.model.conf.MitIdConf;
import dk.acto.fafnir.server.model.conf.TestConf;
import dk.acto.fafnir.server.providers.credentials.TokenCredentials;
import io.vavr.control.Option;
import io.vavr.control.Try;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Slf4j
@Lazy
@Component
@AllArgsConstructor
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
        var code = data.getToken();
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
                .subject(userInfo.getLeft())
                .provider("mitid")
                .name(userInfo.getRight())
                .build());
        return CallbackResult.success(jwt);
    }

    private Pair<String, String> getUserInfo (String token) {
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
