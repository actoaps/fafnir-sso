package dk.acto.auth.providers;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.github.scribejava.apis.openid.OpenIdOAuth2AccessToken;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.oauth.OAuth20Service;
import dk.acto.auth.FailureReason;
import dk.acto.auth.TokenFactory;
import dk.acto.auth.model.CallbackResult;
import dk.acto.auth.model.FafnirUser;
import dk.acto.auth.model.conf.GoogleConf;
import dk.acto.auth.providers.credentials.Token;
import io.vavr.control.Option;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnBean(GoogleConf.class)
public class GoogleProvider implements RedirectingAuthenticationProvider<Token> {
    private final OAuth20Service googleOauth;
    private final TokenFactory tokenFactory;

    public GoogleProvider(@Qualifier("googleOAuth") OAuth20Service googleOauth, TokenFactory tokenFactory) {
        this.googleOauth = googleOauth;
        this.tokenFactory = tokenFactory;
    }

    public String authenticate() {
        return googleOauth.getAuthorizationUrl();
    }

    @Override
    public CallbackResult callback(Token data) {
        var code = data.getToken();
        final OAuth2AccessToken token = Option.of(code)
                .toTry()
                .mapTry(googleOauth::getAccessToken)
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
                .provider("google")
                .name(displayName)
                .build());
        return CallbackResult.success(jwt);
    }
}
