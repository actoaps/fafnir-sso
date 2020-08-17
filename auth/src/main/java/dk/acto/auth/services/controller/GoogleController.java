package dk.acto.auth.services.controller;

import dk.acto.auth.ActoConf;
import dk.acto.auth.providers.GoogleProvider;
import dk.acto.auth.providers.credentials.Token;
import dk.acto.auth.providers.validators.GoogleValidator;
import dk.acto.auth.services.ServiceHelper;
import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;

@RestController
@Slf4j
@RequestMapping("google")
public class GoogleController {
	private final GoogleProvider provider;

	@Autowired
	public GoogleController(GoogleProvider provider, @Validated(GoogleValidator.class) ActoConf actoConf) {
		this.provider = provider;
	}

	@GetMapping
	public void authenticate(HttpServletResponse response) {
		Try.of(() -> ServiceHelper.functionalRedirectTo(response, provider::authenticate));
	}

	@GetMapping("callback")
	public void callback(HttpServletResponse response, @RequestParam String code) {
		Try.of(() -> ServiceHelper.functionalRedirectTo(response, () -> provider.callback(Token.builder()
				.token(code)
				.build())));
	}
}
