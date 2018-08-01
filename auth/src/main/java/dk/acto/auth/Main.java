package dk.acto.auth;

import com.google.gson.Gson;
import dk.acto.auth.providers.FacebookProvider;
import dk.acto.auth.providers.GoogleProvider;
import io.vavr.control.Option;
import lombok.extern.log4j.Log4j2;

import java.util.function.Supplier;

import static spark.Spark.get;
import static spark.Spark.port;

@Log4j2
public class Main {

    private static Gson gson = new Gson();
    private static final TokenFactory TOKEN_FACTORY = new TokenFactory();

    public static void main(String[] args) {
        ActoConf actoConf = Option.of(System.getenv("ACTO_CONF"))
                .toTry().map(x -> gson.fromJson(x, ActoConf.class))
                .onFailure(x -> log.fatal("ACTO_CONF environment variable not found, please supply ione in JSONsimilar to this: \n" + gson.toJson(ActoConf.DEFAULT), x))
                .getOrElseThrow((Supplier<IllegalArgumentException>) IllegalArgumentException::new);

        FacebookProvider facebook = new FacebookProvider(actoConf, TOKEN_FACTORY);
        GoogleProvider google = new GoogleProvider(actoConf, TOKEN_FACTORY);

        if (actoConf.isEmitTestToken()) {
            log.info("Test Token: " + TOKEN_FACTORY.generateToken("test", "test", "Testy McTestface"));
        }

        port(8080);

        get("/facebook", (request, response) -> {
            response.redirect(facebook.authenticate());
            return "";
        });

        get("/callback-facebook", (request, response) -> {
            response.redirect(facebook.callback(request.queryParams("code")));
            return "";
        });

        get("/google", (request, response) -> {
            response.redirect(google.authenticate());
            return "";
        });

        get("/callback-google", (request, response) -> {
            response.redirect(google.callback(request.queryParams("code")));
            return "";
        });

        get("/public-key", (request, response) -> TOKEN_FACTORY.getPublicKey());
    }
}
