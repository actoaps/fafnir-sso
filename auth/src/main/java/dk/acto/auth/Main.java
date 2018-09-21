package dk.acto.auth;

import com.google.gson.Gson;
import dk.acto.auth.providers.FacebookProvider;
import dk.acto.auth.providers.GoogleProvider;
import dk.acto.auth.providers.UniLoginProvider;
import dk.acto.auth.providers.unilogin.Institution;
import dk.acto.auth.providers.validators.FacebookValidator;
import dk.acto.auth.providers.validators.GoogleValidator;
import dk.acto.auth.providers.validators.UniLoginValidator;
import io.vavr.control.Option;
import lombok.extern.log4j.Log4j2;
import spark.ModelAndView;
import spark.template.thymeleaf.ThymeleafTemplateEngine;

import javax.validation.Validation;
import javax.validation.Validator;
import java.util.*;
import java.util.function.Supplier;

import static spark.Spark.*;

@Log4j2
public class Main {

	private static final TokenFactory TOKEN_FACTORY = new TokenFactory();
	private static Gson gson = new Gson();
    private static final Validator VALIDATOR =  Validation.buildDefaultValidatorFactory().getValidator();
	public static void main(String[] args) {
		ActoConf actoConf = Option.of(System.getenv("ACTO_CONF"))
				.toTry().map(x -> gson.fromJson(x, ActoConf.class))
				.onFailure(x -> log.fatal("ACTO_CONF environment variable not found, please supply one in JSON format similar to this: \n" + gson.toJson(ActoConf.DEFAULT), x))
				.getOrElseThrow((Supplier<IllegalArgumentException>) IllegalArgumentException::new);

		FacebookProvider facebook = new FacebookProvider(actoConf, TOKEN_FACTORY);
		GoogleProvider google = new GoogleProvider(actoConf, TOKEN_FACTORY);
		UniLoginProvider unilogin = new UniLoginProvider(actoConf, TOKEN_FACTORY);

		if (actoConf.isEmitTestToken()) {
			log.info("Test Token: " + TOKEN_FACTORY.generateToken("test", "test", "Testy McTestface"));
		}

		port(8080);

		get("/facebook", (request, response) -> {
			if (!VALIDATOR.validate(actoConf, FacebookValidator.class).isEmpty()) {
			    halt(404);
            }

    	    response.redirect(facebook.authenticate());
			return "";
		});

		get("/callback-facebook", (request, response) -> {
            if (!VALIDATOR.validate(actoConf, FacebookValidator.class).isEmpty()) {
                halt(404);
            }

			response.redirect(facebook.callback(request.queryParams("code")));
			return "";
		});

		get("/google", (request, response) -> {
            if (!VALIDATOR.validate(actoConf, GoogleValidator.class).isEmpty()) {
                halt(404);
            }

			response.redirect(google.authenticate());
			return "";
		});

		get("/callback-google", (request, response) -> {
            if (!VALIDATOR.validate(actoConf, GoogleValidator.class).isEmpty()) {
                halt(404);
            }

			response.redirect(google.callback(request.queryParams("code")));
			return "";
		});

		get("/unilogin", (request, response) -> {
            if (!VALIDATOR.validate(actoConf, UniLoginValidator.class).isEmpty()) {
                halt(404);
            }

            response.redirect(unilogin.authenticate());
			return "";
		});

		get("/callback-unilogin", (request, response) -> {
            if (!VALIDATOR.validate(actoConf, UniLoginValidator.class).isEmpty()) {
                halt(404);
            }

			response.redirect(unilogin.callback(
					request.queryParams("user"),
					request.queryParams("timestamp"),
					request.queryParams("auth")
			));
			return "";
		});

		get("/callback-unilogin-choose-organization", (request, response) -> {
            if (!VALIDATOR.validate(actoConf, UniLoginValidator.class).isEmpty()) {
                halt(404);
            }

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

			String localeStr = getLocaleStr(request.headers("Accept-Language"), "da", "en");
			return new ModelAndView(model, "/thymeleaf/ChooseInstitutionUni-Login" + localeStr + ".thymeleaf");
		}, new ThymeleafTemplateEngine());

		post("/callback-unilogin-choose-organization", (request, response) -> {
            if (!VALIDATOR.validate(actoConf, UniLoginValidator.class).isEmpty()) {
                halt(404);
            }

			String auth = request.queryParams("auth");
			String timestamp = request.queryParams("timestamp");
			String user = request.queryParams("user");
			String institutionId = request.queryParams("institution");
			response.redirect(unilogin.callbackWithInstitution(user, timestamp, auth, institutionId));
			return "";
		});

		get("/public-key", (request, response) -> TOKEN_FACTORY.getPublicKey());
	}

	/**
	 * @param acceptLanguageHeader
	 * @param acceptedLocales
	 * @return default "en" and is empty
	 */
	private static String getLocaleStr(String acceptLanguageHeader, String... acceptedLocales) {
		Locale foundLocale = getSupportedLocale(acceptLanguageHeader, acceptedLocales).orElse(Locale.ENGLISH);
		return "en".equals(foundLocale.getLanguage()) ? "" : "_" + foundLocale.getLanguage();
	}

	private static Optional<Locale> getSupportedLocale(String acceptLanguageHeader, String... acceptedLocales) {
        return getSupportedLocale(
                acceptLanguageHeader,
                Arrays.stream(acceptedLocales).map(Locale::forLanguageTag).toArray(Locale[]::new)
        );
	}

	private static Optional<Locale> getSupportedLocale(String acceptLanguageHeader, Locale[] acceptedLocales) {
		List<Locale.LanguageRange> languages = Locale.LanguageRange.parse(acceptLanguageHeader);
		return Optional.ofNullable(Locale.lookup(languages, Arrays.asList(acceptedLocales)));
	}
}
