package dk.acto.fafnir.sso.provider;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.Claim;
import com.github.scribejava.core.oauth.OAuth20Service;
import dk.acto.fafnir.api.exception.MSIdentityAttributeMissing;
import dk.acto.fafnir.api.model.*;
import dk.acto.fafnir.api.provider.RedirectingAuthenticationProvider;
import dk.acto.fafnir.api.provider.metadata.MetadataProvider;
import dk.acto.fafnir.api.service.AdministrationService;
import dk.acto.fafnir.sso.provider.credentials.TokenCredentials;
import dk.acto.fafnir.sso.util.TokenFactory;
import io.vavr.control.Try;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.security.SecureRandom;
import java.util.Map;
import java.util.Optional;

@Slf4j
@AllArgsConstructor
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
        var token = Try.of(() -> JWT.decode(data.getCode()))
                .onFailure(x -> log.error("Authentication failed", x))
                .getOrNull();

        if (token == null) {
            return AuthenticationResult.failure(FailureReason.AUTHENTICATION_FAILED);
        }

        var subject = Optional.ofNullable(token.getClaim("email"))
                .map(Claim::asString)
                .orElseThrow(MSIdentityAttributeMissing::new);
        var displayName = Optional.ofNullable(token.getClaim("name"))
                .map(Claim::asString)
                .orElseThrow(MSIdentityAttributeMissing::new);
        var tenantId = Optional.ofNullable(token.getClaim("tid"))
                .map(Claim::asString)
                .orElseThrow(MSIdentityAttributeMissing::new);

        var subjectActual = UserData.builder()
                .subject(subject)
                .name(displayName)
                .build();
        var orgActual = administrationService.readOrganisation(test -> tenantId.equals(test.getValues().get("TenantId")));
        var claimsActual = ClaimData.empty();

        var jwt = tokenFactory.generateToken(subjectActual, orgActual, claimsActual, getMetaData());

        return AuthenticationResult.success(jwt);
    }

    @Override
    public ProviderMetaData getMetaData() {
        return MetadataProvider.MS_IDENTITY;
    }
}
