package dk.acto.fafnir.server.provider;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.github.scribejava.apis.openid.OpenIdOAuth2AccessToken;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.oauth.OAuth20Service;
import dk.acto.fafnir.api.model.*;
import dk.acto.fafnir.api.service.AdministrationService;
import dk.acto.fafnir.server.model.FailureReason;
import dk.acto.fafnir.server.util.TokenFactory;
import dk.acto.fafnir.server.model.CallbackResult;
import dk.acto.fafnir.server.provider.credentials.TokenCredentials;
import io.vavr.control.Option;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@AllArgsConstructor
@ConditionalOnBean(name = "googleOAuth")
public class GoogleProvider implements RedirectingAuthenticationProvider<TokenCredentials> {
    @Qualifier("googleOAuth")
    private final OAuth20Service googleOauth;
    private final TokenFactory tokenFactory;
    private final AdministrationService administrationService;

    public String authenticate() {
        return googleOauth.getAuthorizationUrl();
    }

    @Override
    public CallbackResult callback(TokenCredentials data) {
        var code = data.getCode();
        final OAuth2AccessToken token = Option.of(code)
                .toTry()
                .mapTry(googleOauth::getAccessToken)
                .onFailure(x -> log.error("Authentication failed", x))
                .getOrNull();
        if (token == null) {
            return CallbackResult.failure(FailureReason.AUTHENTICATION_FAILED);
        }

        DecodedJWT jwtToken = JWT.decode(((OpenIdOAuth2AccessToken) token).getOpenIdToken());
        var subject = jwtToken.getClaims().get("email").asString();
        var displayName = jwtToken.getClaims().get("name").asString();
        var providerValue = jwtToken.getClaim("hd").asString();

        var subjectActual = UserData.builder()
                .subject(subject)
                .name(displayName)
                .build();
        var orgActual = administrationService.readOrganisation("hd", providerValue);
        var claimsActual = ClaimData.empty(subjectActual.getSubject(), orgActual.getOrganisationId());

        String jwt = tokenFactory.generateToken(subjectActual, orgActual, claimsActual, getMetaData());

        return CallbackResult.success(jwt);
    }

    @Override
    public String providerId() {
        return "google";
    }

    @Override
    public ProviderMetaData getMetaData() {
        return ProviderMetaData.builder()
                .providerName("Google")
                .providerId(providerId())
                .organisationSupport(OrganisationSupport.NATIVE)
                .inputs(List.of("Organisation Domain"))
                .build();
    }
}
