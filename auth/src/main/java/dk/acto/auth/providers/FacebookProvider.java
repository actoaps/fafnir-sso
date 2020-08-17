package dk.acto.auth.providers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.scribejava.apis.FacebookApi;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;
import dk.acto.auth.ActoConf;
import dk.acto.auth.TokenFactory;
import dk.acto.auth.model.FafnirUser;
import dk.acto.auth.providers.credentials.Token;
import io.vavr.control.Option;
import io.vavr.control.Try;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Log4j2
@Component
public class FacebookProvider implements RedirectingAuthenticationProvider<Token> {
    private final ActoConf actoConf;
    private final OAuth20Service facebookService;
    private final TokenFactory tokenFactory;
    private final ObjectMapper objectMapper;

    @Autowired
    public FacebookProvider(ActoConf actoConf, TokenFactory tokenFactory, ObjectMapper objectMapper) {
        this.actoConf = actoConf;
        this.facebookService = Try.of(() -> new ServiceBuilder(actoConf.getFacebookAppId())
                .apiSecret(actoConf.getFacebookSecret())
                .callback(actoConf.getMyUrl() + "/facebook/callback")
                .defaultScope("email")
                .build(FacebookApi.instance())).getOrNull();
        this.tokenFactory = tokenFactory;
        this.objectMapper = objectMapper;
    }

    public String authenticate() {
        return facebookService.getAuthorizationUrl();
    }

    public String callback(Token data) {
        var code = data.getToken();
        OAuth2AccessToken token = Option.of(code)
                .toTry()
                .mapTry(facebookService::getAccessToken)
                .onFailure(x -> log.error("Authentication failed", x))
                .getOrNull();
        if (token == null) {
            return actoConf.getFailureUrl();
        }

        final OAuthRequest facebookRequest = new OAuthRequest(Verb.GET, "https://graph.facebook.com/v3.0/me?fields=email,name,id");
        facebookService.signRequest(token, facebookRequest);
        var result = Try.of(() -> facebookService.execute(facebookRequest).getBody())
                .mapTry(objectMapper::readTree)
                .getOrNull();
        String subject = result.get("email").asText();
        String name = result.get("name").asText();
        String id = result.get("id").asText();
        if (subject == null || subject.isEmpty()) {
            return actoConf.getFailureUrl();
        }

        String jwt = tokenFactory.generateToken(FafnirUser.builder()
                .subject(subject)
                .provider("facebook")
                .name(name)
                .metaId(id)
                .build());
        return actoConf.getSuccessUrl() + "#" + jwt;
    }
}

