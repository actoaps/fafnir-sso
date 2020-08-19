package dk.acto.auth.providers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;
import dk.acto.auth.FailureReason;
import dk.acto.auth.TokenFactory;
import dk.acto.auth.model.CallbackResult;
import dk.acto.auth.model.FafnirUser;
import dk.acto.auth.model.conf.FacebookConf;
import dk.acto.auth.providers.credentials.Token;
import io.vavr.control.Option;
import io.vavr.control.Try;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

@Log4j2
@Component
@ConditionalOnBean(FacebookConf.class)
public class FacebookProvider implements RedirectingAuthenticationProvider<Token> {
    private final TokenFactory tokenFactory;
    private final ObjectMapper objectMapper;
    private final OAuth20Service facebookOauth;

    public FacebookProvider(TokenFactory tokenFactory, ObjectMapper objectMapper, @Qualifier("facebookOAuth") OAuth20Service facebookOauth) {
        this.tokenFactory = tokenFactory;
        this.objectMapper = objectMapper;
        this.facebookOauth = facebookOauth;
    }

    public String authenticate() {
        return facebookOauth.getAuthorizationUrl();
    }

    public CallbackResult callback(Token data) {
        var code = data.getToken();
        OAuth2AccessToken token = Option.of(code)
                .toTry()
                .mapTry(facebookOauth::getAccessToken)
                .onFailure(x -> log.error("Authentication failed", x))
                .getOrNull();
        if (token == null) {
            return CallbackResult.failure(FailureReason.AUTHENTICATION_FAILED);
        }

        final OAuthRequest facebookRequest = new OAuthRequest(Verb.GET, "https://graph.facebook.com/v3.0/me?fields=email,name,id");
        facebookOauth.signRequest(token, facebookRequest);
        var result = Try.of(() -> facebookOauth.execute(facebookRequest).getBody())
                .mapTry(objectMapper::readTree)
                .getOrNull();
        String subject = result.get("email").asText();
        String name = result.get("name").asText();
        String id = result.get("id").asText();
        if (subject == null || subject.isEmpty()) {
            return CallbackResult.failure(FailureReason.AUTHENTICATION_FAILED);
        }

        String jwt = tokenFactory.generateToken(FafnirUser.builder()
                .subject(subject)
                .provider("facebook")
                .name(name)
                .metaId(id)
                .build());
        return CallbackResult.success(jwt);
    }
}
