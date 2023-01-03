package dk.acto.fafnir.sso.provider;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.Claim;
import com.github.scribejava.apis.openid.OpenIdOAuth2AccessToken;
import com.github.scribejava.core.oauth.OAuth20Service;
import dk.acto.fafnir.api.model.*;
import dk.acto.fafnir.api.provider.RedirectingAuthenticationProvider;
import dk.acto.fafnir.api.provider.metadata.MetadataProvider;
import dk.acto.fafnir.api.service.AdministrationService;
import dk.acto.fafnir.sso.provider.credentials.TokenCredentials;
import dk.acto.fafnir.sso.util.TokenFactory;
import io.vavr.control.Option;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
@AllArgsConstructor
public class GoogleProvider implements RedirectingAuthenticationProvider<TokenCredentials> {
    private final OAuth20Service googleOauth;
    private final TokenFactory tokenFactory;
    private final AdministrationService administrationService;

    public String authenticate() {
        return googleOauth.getAuthorizationUrl();
    }

    @Override
    public AuthenticationResult callback(TokenCredentials data) {
        var code = data.getCode();
        final var token = Option.of(code)
                .toTry()
                .mapTry(googleOauth::getAccessToken)
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
                .subject(subject)
                .name(displayName)
                .build();
        var orgActual = administrationService.readOrganisation(
                test -> providerValue.equals(test.getValues().get("Organisation Domain")) || "true".equals(test.getValues().get("Catchall Organisation"))
        );
        var claimsActual = ClaimData.empty();

        var jwt = tokenFactory.generateToken(subjectActual, orgActual, claimsActual, getMetaData());

        return AuthenticationResult.success(jwt);
    }

    @Override
    public ProviderMetaData getMetaData() {
        return MetadataProvider.GOOGLE;
    }
}
