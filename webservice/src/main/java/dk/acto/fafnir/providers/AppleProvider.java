package dk.acto.fafnir.providers;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.github.scribejava.apis.openid.OpenIdOAuth2AccessToken;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.oauth.OAuth20Service;
import dk.acto.fafnir.FailureReason;
import dk.acto.fafnir.TokenFactory;
import dk.acto.fafnir.model.CallbackResult;
import dk.acto.fafnir.model.FafnirUser;
import dk.acto.fafnir.providers.credentials.TokenCredentials;
import io.vavr.control.Option;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@Lazy
public class AppleProvider implements RedirectingAuthenticationProvider<TokenCredentials>{
    private final OAuth20Service appleOauth;
    private final TokenFactory tokenFactory;

    public AppleProvider(@Qualifier("appleOAuth") OAuth20Service appleOauth, TokenFactory tokenFactory) {
        this.appleOauth = appleOauth;
        this.tokenFactory = tokenFactory;
    }

    public String authenticate() {
        log.info(appleOauth.getCallback());
        return appleOauth.getAuthorizationUrl(Map.of("response_mode", "form_post"));
    }

    @Override
    public CallbackResult callback(TokenCredentials data) {
        var code = data.getToken();
        final OAuth2AccessToken token = Option.of(code)
                .toTry()
                .mapTry(appleOauth::getAccessToken)
                .onFailure(x -> log.error("Authentication failed", x))
                .getOrNull();
        if (token == null) {
            return CallbackResult.failure(FailureReason.AUTHENTICATION_FAILED);
        }

        DecodedJWT jwtToken = JWT.decode(((OpenIdOAuth2AccessToken) token).getOpenIdToken());
        String subject = jwtToken.getClaims().get("email").asString();
        String displayName = jwtToken.getClaims().get("name").asString();

        String jwt = tokenFactory.generateToken(FafnirUser.builder()
                .subject(subject)
                .provider("apple")
                .name(displayName)
                .build());
        return CallbackResult.success(jwt);
    }
}
