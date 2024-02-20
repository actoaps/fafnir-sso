package dk.acto.fafnir.sso.provider;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.Claim;
import com.github.scribejava.apis.openid.OpenIdOAuth2AccessToken;
import com.github.scribejava.core.oauth.OAuth20Service;
import dk.acto.fafnir.api.model.*;
import dk.acto.fafnir.api.provider.RedirectingAuthenticationProvider;
import dk.acto.fafnir.api.provider.metadata.MetadataProvider;
import dk.acto.fafnir.api.service.AdministrationService;
import dk.acto.fafnir.sso.model.conf.ProviderConf;
import dk.acto.fafnir.sso.provider.credentials.TokenCredentials;
import dk.acto.fafnir.sso.util.TokenFactory;
import io.vavr.control.Try;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
@AllArgsConstructor
public class GoogleProvider implements RedirectingAuthenticationProvider<TokenCredentials> {
    private final OAuth20Service googleOauth;
    private final TokenFactory tokenFactory;
    private final AdministrationService administrationService;
    private final ProviderConf providerConf;

    public String authenticate() {
        return googleOauth.getAuthorizationUrl();
    }

    @Override
    public AuthenticationResult callback(TokenCredentials data) {
        var token = Try.of(() -> googleOauth.getAccessToken(data.getCode()))
            .onFailure(x -> log.error("Authentication failed", x))
            .getOrNull();
        if (token == null) {
            return AuthenticationResult.failure(FailureReason.AUTHENTICATION_FAILED);
        }

        var jwtToken = JWT.decode(((OpenIdOAuth2AccessToken) token).getOpenIdToken());
        var subject = jwtToken.getClaims().get("email").asString();
        var displayName = jwtToken.getClaims().get("name").asString();
        var providerValue = Optional.ofNullable(jwtToken.getClaim("hd"))
            .map(Claim::asString)
            .orElse("");

        var subjectActual = UserData.builder()
            .subject(providerConf.applySubjectRules(subject))
            .name(displayName)
            .build();

        var orgOptional = administrationService.readOrganisationDoesNotThrow(
            test -> getMetaData().getProviderId().equals(test.getProviderId()) &&
                (providerValue.equals(test.getValues().get("Organisation Domain")) || "true".equals(test.getValues().get("Catchall Organisation")))
        );

        if (orgOptional.isPresent()) {
            var orgActual = orgOptional.get();
            var claimsActual = ClaimData.empty();
            var jwt = tokenFactory.generateToken(subjectActual, orgActual, claimsActual, getMetaData(), providerValue);
            return AuthenticationResult.success(jwt);
        } else {
            var fafnirUser = FafnirUser.builder()
                .data(subjectActual)
                .organisationId(providerValue)
                .organisationName(displayName)
                .provider("google")
                .build();

            var jwt = tokenFactory.generateToken(fafnirUser);
            return AuthenticationResult.success(jwt);
        }
    }

    @Override
    public ProviderMetaData getMetaData() {
        return MetadataProvider.GOOGLE;
    }
}
