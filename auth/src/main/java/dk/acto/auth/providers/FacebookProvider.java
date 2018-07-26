package dk.acto.auth.providers;

import com.github.scribejava.apis.FacebookApi;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;
import com.google.gson.JsonParser;
import dk.acto.auth.ActoConf;
import dk.acto.auth.TokenFactory;
import io.vavr.control.Option;
import io.vavr.control.Try;
import lombok.extern.log4j.Log4j2;

import java.util.UUID;

@Log4j2
public class FacebookProvider {
    private final ActoConf actoConf;
    private final OAuth20Service facebookService;
    private final TokenFactory tokenFactory;
    private JsonParser jsonParser = new JsonParser();

    public FacebookProvider(ActoConf actoConf, TokenFactory tokenFactory) {
        this.actoConf = actoConf;
        this.facebookService = new ServiceBuilder(actoConf.getFacebookAppId())
                .apiSecret(actoConf.getFacebookSecret())
                .state(UUID.randomUUID().toString())
                .callback(actoConf.getMyUrl() + "/callback-facebook")
                .scope("email")
                .build(FacebookApi.instance());
        this.tokenFactory = tokenFactory;
    }

    public String authenticate() {
        return facebookService.getAuthorizationUrl();
    }

    public String callback(String code) {
        OAuth2AccessToken token = Option.of(code)
                .toTry()
                .mapTry(facebookService::getAccessToken)
                .onFailure(x -> log.error("Authentication failed", x))
                .getOrNull();
        if (token == null) {
            return actoConf.getFailureUrl();
        }

        final OAuthRequest facebookRequest = new OAuthRequest(Verb.GET, "https://graph.facebook.com/v3.0/me?fields=email");
        facebookService.signRequest(token, facebookRequest);
        String subject = Try.of(() -> facebookService.execute(facebookRequest).getBody())
                .mapTry(x -> jsonParser.parse(x).getAsJsonObject().get("email").getAsString())
                .getOrNull();
        if (subject == null || subject.isEmpty()) {
            return actoConf.getFailureUrl();
        }

        String jwt = tokenFactory.generateToken(subject);
        return actoConf.getSuccessUrl() + "#" + jwt;


    }
}

