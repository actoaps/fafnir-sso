package dk.acto.auth.services;

import dk.acto.auth.ActoConf;
import dk.acto.auth.providers.TestProvider;
import dk.acto.auth.providers.validators.TestValidator;
import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;

@RestController
@Slf4j
@RequestMapping("test")
public class TestService {
	private final TestProvider provider;

	@Autowired
	public TestService(TestProvider provider, @Validated(TestValidator.class) ActoConf actoConf) {
		this.provider = provider;
	}

	@GetMapping
	public void authenticate(HttpServletResponse response) {
		Try.of(() -> ServiceHelper.functionalRedirectTo(response, provider::authenticate));
	}
}
