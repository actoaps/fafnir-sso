package dk.acto.fafnir.services.controller;

import dk.acto.fafnir.providers.TestProvider;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletResponse;

@RestController
@ConditionalOnProperty(name = "TEST_ENABLED")
@Slf4j
@RequestMapping("test")
@AllArgsConstructor
public class TestController {
	private final TestProvider provider;

	@GetMapping
	public RedirectView authenticate(HttpServletResponse response) {
		return new RedirectView(provider.authenticate());
	}

	@PostConstruct
	private void postConstruct() {
		log.info("Exposing Test Endpoint...");
	}

}
