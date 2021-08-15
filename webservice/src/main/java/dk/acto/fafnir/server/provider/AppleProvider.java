package dk.acto.fafnir.server.provider;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.github.scribejava.core.oauth.OAuth20Service;
import dk.acto.fafnir.server.TokenFactory;
import dk.acto.fafnir.server.model.CallbackResult;
import dk.acto.fafnir.api.model.FafnirUser;
import dk.acto.fafnir.server.provider.credentials.TokenCredentials;
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
        return appleOauth.getAuthorizationUrl(Map.of("response_mode", "form_post"));
    }

    @Override
    public CallbackResult callback(TokenCredentials data) {
        var code = data.getIdToken();

        DecodedJWT jwtToken = JWT.decode(code);
        String subject = jwtToken.getClaims().get("sub").asString();
        String displayName = jwtToken.getClaims().get("email").asString();

        String jwt = tokenFactory.generateToken(FafnirUser.builder()
                .subject(subject)
                .provider("apple")
                .name(displayName)
                .metaId(subject)
                .build());
        return CallbackResult.success(jwt);
    }
}
