package dk.acto.auth;

import com.google.gson.Gson;
import dk.acto.auth.providers.FacebookProvider;
import dk.acto.auth.providers.GoogleProvider;
import dk.acto.auth.providers.UniLoginProvider;
import dk.acto.auth.providers.unilogin.Institution;
import io.vavr.control.Option;
import lombok.extern.log4j.Log4j2;
import spark.ModelAndView;
import spark.template.thymeleaf.ThymeleafTemplateEngine;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static spark.Spark.get;
import static spark.Spark.port;
import static spark.Spark.post;

@Log4j2
public class Main {

	private static final TokenFactory TOKEN_FACTORY = new TokenFactory();
	private static Gson gson = new Gson();

	public static void main(String[] args) {
		ActoConf actoConf = Option.of(System.getenv("ACTO_CONF"))
				.toTry().map(x -> gson.fromJson(x, ActoConf.class))
				.onFailure(x -> log.fatal("ACTO_CONF environment variable not found, please supply ione in JSONsimilar to this: \n" + gson.toJson(ActoConf.DEFAULT), x))
				.getOrElseThrow((Supplier<IllegalArgumentException>) IllegalArgumentException::new);

		FacebookProvider facebook = new FacebookProvider(actoConf, TOKEN_FACTORY);
		GoogleProvider google = new GoogleProvider(actoConf, TOKEN_FACTORY);
		UniLoginProvider unilogin = new UniLoginProvider(actoConf, TOKEN_FACTORY);

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

		get("/unilogin", (request, response) -> {
			response.redirect(unilogin.authenticate());
			return "";
		});

		get("/callback-unilogin", (request, response) -> {
				response.redirect(unilogin.callback(
						request.queryParams("user"),
						request.queryParams("timestamp"),
						request.queryParams("auth")
				));
			return "";
		});

		get("/callback-unilogin-choose-organization", (request, response) -> {
			String auth = request.queryParams("auth");
			String timestamp = request.queryParams("timestamp");
			String user = request.queryParams("user");
			//unilogin.isValid()
			List<Institution> institutionList = unilogin.getInstitutionList(user);
			Map<String, Object> model = new HashMap<>();
			model.put("auth", auth);
			model.put("timestamp", timestamp);
			model.put("user", user);
			model.put("institutionList", institutionList);
			return new ModelAndView(model, "/thymeleaf/ChooseInstitutionUni-Login.thymeleaf");
		}, new ThymeleafTemplateEngine());

		post("/callback-unilogin-choose-organization", (request, response) -> {
			String auth = request.queryParams("auth");
			String timestamp = request.queryParams("timestamp");
			String user = request.queryParams("user");
			String institutionId = request.queryParams("institutionId");
			response.redirect(unilogin.callbackWithInstitution(user, timestamp, auth, institutionId));
			return "";
		});

		get("/public-key", (request, response) -> TOKEN_FACTORY.getPublicKey());
	}
}
