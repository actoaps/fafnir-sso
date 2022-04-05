package dk.acto.fafnir.server.provider;

import com.auth0.jwt.JWT;
import com.github.scribejava.core.oauth.OAuth20Service;
import dk.acto.fafnir.api.model.FafnirUser;
import dk.acto.fafnir.api.model.UserData;
import dk.acto.fafnir.server.model.FailureReason;
import dk.acto.fafnir.server.util.TokenFactory;
import dk.acto.fafnir.server.model.CallbackResult;
import dk.acto.fafnir.server.provider.credentials.TokenCredentials;
import io.vavr.control.Option;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Map;

@Slf4j
@Component
@AllArgsConstructor
@ConditionalOnBean(name = "microsoftIdentityOauth")
public class MicrosoftIdentityProvider implements RedirectingAuthenticationProvider<TokenCredentials> {
    private final TokenFactory tokenFactory;
    private final OAuth20Service microsoftIdentityOauth;

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
        var token = Option.of(data.getCode())
                .toTry()
                .mapTry(JWT::decode)
                .onFailure(x -> log.error("Authentication failed", x))
                .getOrNull();

        if (token == null) {
            return CallbackResult.failure(FailureReason.AUTHENTICATION_FAILED);
        }

        var subject = token.getClaims().get("email").asString();
        var displayName = token.getClaims().get("name").asString();
        var tenantId = token.getClaim("tid").asString();

        String jwt = tokenFactory.generateToken(FafnirUser.builder().
                        data(UserData.builder()
                                        .subject(subject)
                                        .provider("msidentity")
                                        .name(displayName)
                                        .build()
                        )
                .organisationId(tenantId.equals(PERSONAL_TENANT_GUID) ? null : tenantId)
                .build());

        return CallbackResult.success(jwt);
    }

    @Override
    public boolean supportsOrganisationUrls() {
        return false;
    }

    @Override
    public String entryPoint() {
        return "msidentity";
    }
}
