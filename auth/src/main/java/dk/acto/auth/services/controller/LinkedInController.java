package dk.acto.auth.services.controller;

import dk.acto.auth.model.conf.FafnirConf;
import dk.acto.auth.providers.LinkedInProvider;
import dk.acto.auth.providers.credentials.Token;
import dk.acto.auth.services.ServiceHelper;
import io.vavr.control.Try;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletResponse;

@Controller
@Slf4j
@RequestMapping("linkedin")
@ConditionalOnBean(LinkedInProvider.class)
@AllArgsConstructor
public class LinkedInController {
	private final LinkedInProvider provider;
	private final FafnirConf fafnirConf;

	@GetMapping
	public RedirectView authenticate(HttpServletResponse response) {
		return new RedirectView(provider.authenticate());
	}

	@GetMapping("callback")
	public RedirectView callback(HttpServletResponse response, @RequestParam String code) {
		return new RedirectView(provider.callback(Token.builder()
				.token(code)
				.build()).getUrl(fafnirConf));
	}
}
