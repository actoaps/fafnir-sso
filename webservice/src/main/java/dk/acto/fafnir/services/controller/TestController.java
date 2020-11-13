package dk.acto.fafnir.services.controller;

import dk.acto.fafnir.model.conf.TestConf;
import dk.acto.fafnir.providers.TestProvider;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletResponse;

@RestController
@Slf4j
@RequestMapping("test")
@ConditionalOnBean(TestConf.class)
@AllArgsConstructor
public class TestController {
	private final TestProvider provider;

	@GetMapping
	public RedirectView authenticate(HttpServletResponse response) {
		return new RedirectView(provider.authenticate());
	}
}
