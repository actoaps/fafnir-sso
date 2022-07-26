package dk.acto.fafnir.sso.provider;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.github.scribejava.apis.openid.OpenIdOAuth2AccessToken;
import com.github.scribejava.core.model.OAuth2AccessToken;
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.util.List;

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
        final OAuth2AccessToken token = Option.of(code)
                .toTry()
                .mapTry(googleOauth::getAccessToken)
                .onFailure(x -> log.error("Authentication failed", x))
                .getOrNull();
        if (token == null) {
            return AuthenticationResult.failure(FailureReason.AUTHENTICATION_FAILED);
        }

        DecodedJWT jwtToken = JWT.decode(((OpenIdOAuth2AccessToken) token).getOpenIdToken());
        var subject = jwtToken.getClaims().get("email").asString();
        var displayName = jwtToken.getClaims().get("name").asString();
        var providerValue = jwtToken.getClaim("hd").asString();

        var subjectActual = UserData.builder()
                .subject(subject)
                .name(displayName)
                .build();
        var orgActual = administrationService.readOrganisation(test -> test.getValues().get("hd").equals(providerValue));
        var claimsActual = ClaimData.empty();

        String jwt = tokenFactory.generateToken(subjectActual, orgActual, claimsActual, getMetaData());

        return AuthenticationResult.success(jwt);
    }

    @Override
    public ProviderMetaData getMetaData() {
        return MetadataProvider.GOOGLE;
    }
}
