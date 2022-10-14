package dk.acto.fafnir.sso.provider;

import com.auth0.jwt.JWT;
import com.github.scribejava.core.oauth.OAuth20Service;
import dk.acto.fafnir.api.model.*;
import dk.acto.fafnir.api.provider.RedirectingAuthenticationProvider;
import dk.acto.fafnir.api.provider.metadata.MetadataProvider;
import dk.acto.fafnir.sso.provider.credentials.TokenCredentials;
import dk.acto.fafnir.sso.util.TokenFactory;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
@AllArgsConstructor
public class AppleProvider implements RedirectingAuthenticationProvider<TokenCredentials> {
    private final OAuth20Service appleOauth;
    private final TokenFactory tokenFactory;

    public String authenticate() {
        return appleOauth.getAuthorizationUrl(Map.of("response_mode", "form_post"));
    }

    @Override
    public AuthenticationResult callback(TokenCredentials data) {
        var code = data.getCode();

        var jwtToken = JWT.decode(code);
        var subject = jwtToken.getClaims().get("sub").asString();
        var displayName = jwtToken.getClaims().get("email").asString();

        var subjectActual = UserData.builder()
                .subject(subject)
                .name(displayName)
                .build();
        var orgActual = OrganisationData.DEFAULT;
        var claimsActual = ClaimData.empty();

        var jwt = tokenFactory.generateToken(subjectActual, orgActual, claimsActual, getMetaData());

        return AuthenticationResult.success(jwt);
    }

    @Override
    public ProviderMetaData getMetaData() {
        return MetadataProvider.APPLE;
    }
}
