package dk.acto.auth;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import dk.acto.auth.providers.FacebookProvider;
import io.vavr.control.Option;
import lombok.extern.log4j.Log4j2;

import java.util.function.Supplier;

import static spark.Spark.get;
import static spark.Spark.port;

@Log4j2
public class Main {

    private static JsonParser jsonParser = new JsonParser();
    private static Gson gson = new Gson();
    private static final TokenFactory TOKEN_FACTORY = new TokenFactory();
    private static FacebookProvider facebook;

    public static void main(String[] args) {
        ActoConf actoConf = Option.of(System.getenv("ACTO_CONF"))
                .toTry().map(x -> gson.fromJson(x, ActoConf.class))
                .onFailure(x -> log.fatal("ACTO_CONF environment variable not found, please supply ione in JSONsimilar to this: \n" + gson.toJson(ActoConf.DEFAULT), x))
                .getOrElseThrow((Supplier<IllegalArgumentException>) IllegalArgumentException::new);

        facebook = new FacebookProvider(actoConf, TOKEN_FACTORY);

        port(8080);

        get("/facebook", (request, response) -> {
            response.redirect(facebook.authenticate());
            return "";
        });

        get("/callback-facebook", (request, response) -> {
            response.redirect(facebook.callback(request.queryParams("code")));
            return "";
        });

        get("/public-key", (request, response) -> TOKEN_FACTORY.getPublicKey());

    }
}
