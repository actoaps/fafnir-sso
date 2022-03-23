package dk.acto.fafnir.server.service.controller;

import dk.acto.fafnir.server.provider.TestProvider;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletResponse;

@RestController
@Slf4j
@RequestMapping("test")
@AllArgsConstructor
@ConditionalOnBean(TestProvider.class)
public class TestController {
	private final TestProvider provider;

	@GetMapping
	public RedirectView authenticate() {
		return new RedirectView(provider.authenticate());
	}

	@PostConstruct
	private void postConstruct() {
		log.info("Exposing Test Endpoint...");
	}

}
