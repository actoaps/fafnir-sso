package dk.acto.auth.providers;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.github.scribejava.apis.GoogleApi20;
import com.github.scribejava.apis.openid.OpenIdOAuth2AccessToken;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.oauth.OAuth20Service;
import dk.acto.auth.ActoConf;
import dk.acto.auth.TokenFactory;
import io.vavr.control.Option;
import io.vavr.control.Try;
import lombok.extern.log4j.Log4j2;

import java.util.UUID;

@Log4j2
public class GoogleProvider {
    private final ActoConf actoConf;
    private final OAuth20Service googleService;
    private final TokenFactory tokenFactory;

    public GoogleProvider(ActoConf actoConf, TokenFactory tokenFactory) {
        this.actoConf = actoConf;
        this.googleService = Try.of (() ->new ServiceBuilder(actoConf.getGoogleAppId())
                .apiSecret(actoConf.getGoogleSecret())
                .state(UUID.randomUUID().toString())
                .callback(actoConf.getMyUrl() + "/callback-google")
                .scope("openid email profile")
                .build(GoogleApi20.instance())).getOrNull();
        this.tokenFactory = tokenFactory;
    }

    public String authenticate() {
        return googleService.getAuthorizationUrl();
    }

    public String callback(String code) {
        final OAuth2AccessToken token = Option.of(code)
                .toTry()
                .mapTry(googleService::getAccessToken)
                .onFailure(x -> log.error("Authentication failed", x))
                .getOrNull();
        if (token == null) {
            return actoConf.getFailureUrl();
        }

        DecodedJWT jwtToken = JWT.decode(OpenIdOAuth2AccessToken.class.cast(token).getOpenIdToken());
        String subject = jwtToken.getClaims().get("email").asString();
        String displayName = jwtToken.getClaims().get("name").asString();

        String jwt = tokenFactory.generateToken(subject,"google", displayName);
        return actoConf.getSuccessUrl() + "#" + jwt;
    }

}
