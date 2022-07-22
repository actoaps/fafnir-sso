package dk.acto.fafnir.sso.provider;

import com.auth0.jwt.JWT;
import com.github.scribejava.core.oauth.OAuth20Service;
import dk.acto.fafnir.api.model.*;
import dk.acto.fafnir.api.provider.RedirectingAuthenticationProvider;
import dk.acto.fafnir.api.provider.metadata.MetadataProvider;
import dk.acto.fafnir.api.service.AdministrationService;
import dk.acto.fafnir.api.model.FailureReason;
import dk.acto.fafnir.sso.util.TokenFactory;
import dk.acto.fafnir.api.model.AuthenticationResult;
import dk.acto.fafnir.sso.provider.credentials.TokenCredentials;
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
    private final AdministrationService administrationService;
    private final SecureRandom random = new SecureRandom();

    @Override
    public String authenticate() {
        return microsoftIdentityOauth.getAuthorizationUrl(Map.of(
                "nonce", String.valueOf(random.nextInt()),
                "response_mode", "form_post"
        ));
    }

    @Override
    public AuthenticationResult callback(TokenCredentials data) {
        var token = Option.of(data.getCode())
                .toTry()
                .mapTry(JWT::decode)
                .onFailure(x -> log.error("Authentication failed", x))
                .getOrNull();

        if (token == null) {
            return AuthenticationResult.failure(FailureReason.AUTHENTICATION_FAILED);
        }

        var subject = token.getClaims().get("email").asString();
        var displayName = token.getClaims().get("name").asString();
        var tenantId = token.getClaim("tid").asString();

        var subjectActual = UserData.builder()
                .subject(subject)
                .name(displayName)
                .build();
        var orgActual = administrationService.readOrganisation(test -> test.getValues().get("TenantId").equals(tenantId));
        var claimsActual = ClaimData.empty();

        String jwt = tokenFactory.generateToken(subjectActual, orgActual, claimsActual, getMetaData());

        return AuthenticationResult.success(jwt);
    }

    @Override
    public ProviderMetaData getMetaData() {
        return MetadataProvider.MS_IDENTITY;
    }
}
