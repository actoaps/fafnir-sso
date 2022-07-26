package dk.acto.fafnir.sso.service.controller;

import dk.acto.fafnir.sso.provider.TestProvider;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.view.RedirectView;

import javax.annotation.PostConstruct;

@Controller
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
