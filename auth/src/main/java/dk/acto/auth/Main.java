package dk.acto.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.github.scribejava.apis.FacebookApi;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;
import com.google.gson.Gson;
import com.google.gson.JsonParser;
import io.vavr.control.Option;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.codec.binary.Base64;

import java.util.UUID;
import java.util.function.Supplier;

import static spark.Spark.get;
import static spark.Spark.port;

@Log4j2
public class Main {

    private static JsonParser jsonParser = new JsonParser();
    private static Gson gson = new Gson();

    public static void main(String[] args) {
        ActoConf actoConf = Option.of(System.getenv("ACTO_CONF"))
                .toTry().map(x -> gson.fromJson(x, ActoConf.class))
                .onFailure(x -> log.fatal("ACTO_CONF environment variable not found, please supply ione in JSONsimilar to this: \n" + gson.toJson(ActoConf.DEFAULT), x))
                .getOrElseThrow((Supplier<IllegalArgumentException>) IllegalArgumentException::new);

        port(8080);

        String secretState = UUID.randomUUID().toString();
        final OAuth20Service facebookService = new ServiceBuilder(actoConf.getFacebookAppId())
                .apiSecret(actoConf.getFacebookSecret())
                .state(secretState)
                .callback(actoConf.getMyUrl() + "/callback")
                .scope("email")
                .build(FacebookApi.instance());


        get("/facebook", (request, response) -> {
            final String authorizationUrl = facebookService.getAuthorizationUrl();
            response.redirect(authorizationUrl);
            return "";
        });

        get("/callback", (request, response) -> {
            OAuth2AccessToken token = Option.of(request.queryParams("code"))
                    .toTry()
                    .mapTry(facebookService::getAccessToken)
                    .onFailure(x -> response.redirect(actoConf.getFailureUrl()))
                    .onFailure(x -> log.error("Authentication failed", x))
                    .get();
            final OAuthRequest facebookRequest = new OAuthRequest(Verb.GET, "https://graph.facebook.com/v3.0/me?fields=email");
            facebookService.signRequest(token, facebookRequest);
            final Response facebookResponse = facebookService.execute(facebookRequest);
            String email = jsonParser.parse(facebookResponse.getBody()).getAsJsonObject().get("email").getAsString();

            Algorithm algorithm = Algorithm.HMAC512(Base64.decodeBase64(actoConf.getJwtSecret()));

            String jwt = JWT.create()
                    .withIssuer("acto")
                    .withSubject(email)
                    .sign(algorithm);

            response.redirect(actoConf.getSuccessUrl() + "#" + jwt);
            return "";
        });
    }
}
