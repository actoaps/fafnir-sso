package dk.acto.fafnir.server.provider;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;
import dk.acto.fafnir.api.model.FafnirUser;
import dk.acto.fafnir.server.FailureReason;
import dk.acto.fafnir.server.TokenFactory;
import dk.acto.fafnir.server.model.CallbackResult;
import dk.acto.fafnir.server.provider.credentials.TokenCredentials;
import io.vavr.control.Option;
import io.vavr.control.Try;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Map;

@Log4j2
@Lazy
@Component
@AllArgsConstructor
public class MicrosoftIdentityProvider implements RedirectingAuthenticationProvider<TokenCredentials> {
    private final TokenFactory tokenFactory;
    private final ObjectMapper objectMapper;
    private final OAuth20Service microsoftIdentityOauth;

    private static final String MS_GRAPH_ME = "https://graph.microsoft.com/v1.0/me";
    // From: https://docs.microsoft.com/en-us/azure/active-directory/develop/id-tokens
    private static final String PERSONAL_TENANT_GUID = "9188040d-6c67-4c5b-b112-36a304b66dad";
    private final SecureRandom random = new SecureRandom();

    @Override
    public String authenticate() {
        return microsoftIdentityOauth.getAuthorizationUrl(Map.of(
                "nonce", String.valueOf(random.nextInt()),
                "response_mode", "form_post"
        ));
    }

    @Override
    public CallbackResult callback(TokenCredentials data) {
        log.info("CODE ::: " + data.getCode());

        var code = data.getCode();
        OAuth2AccessToken token = Option.of(code)
                .toTry()
                .mapTry(microsoftIdentityOauth::getAccessToken)
                .onFailure(x -> log.error("Authentication failed", x))
                .getOrNull();

        if (token == null) {
            return CallbackResult.failure(FailureReason.AUTHENTICATION_FAILED);
        }

        final OAuthRequest request = new OAuthRequest(Verb.GET, MS_GRAPH_ME);
        request.getHeaders().put("Accept", "application/json");

        microsoftIdentityOauth.signRequest(token, request);
        var result = Try.of(() -> microsoftIdentityOauth.execute(request).getBody())
                .mapTry(objectMapper::readTree)
                .getOrNull();

        String subject = result.get("userPrincipalName").asText();
        String name = result.get("displayName").asText();

        if (subject == null || subject.isEmpty()) {
            return CallbackResult.failure(FailureReason.AUTHENTICATION_FAILED);
        }

        var id = JWT.decode(data.getIdToken());
        var tenantId = id.getClaim("tid").asString();

        String jwt = tokenFactory.generateToken(FafnirUser.builder()
                .subject(subject)
                .provider("msidentity")
                .name(name)
                .organisationId(tenantId.equals(PERSONAL_TENANT_GUID) ? null : tenantId)
                .build());

        return CallbackResult.success(jwt);
    }
}
