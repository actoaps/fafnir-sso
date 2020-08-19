package dk.acto.auth.services.controller;

import dk.acto.auth.ActoConf;
import dk.acto.auth.providers.EconomicCustomerProvider;
import dk.acto.auth.providers.TestProvider;
import dk.acto.auth.providers.validators.TestValidator;
import dk.acto.auth.services.ServiceHelper;
import io.vavr.control.Try;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletResponse;

@RestController
@Slf4j
@RequestMapping("test")
@ConditionalOnBean(TestProvider.class)
@AllArgsConstructor
public class TestController {
	private final TestProvider provider;

	@GetMapping
	public RedirectView authenticate(HttpServletResponse response) {
		return new RedirectView(provider.authenticate());
	}
}
