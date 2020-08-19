package dk.acto.auth.services.controller;

import dk.acto.auth.model.conf.FafnirConf;
import dk.acto.auth.providers.GoogleProvider;
import dk.acto.auth.providers.credentials.Token;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;

@Controller
@Slf4j
@RequestMapping("google")
@ConditionalOnBean(GoogleProvider.class)
@AllArgsConstructor
public class GoogleController {
	private final GoogleProvider provider;
	private final FafnirConf fafnirConf;

	@GetMapping
	public RedirectView authenticate() {
		return new RedirectView(provider.authenticate());
	}

	@GetMapping("callback")
	public RedirectView callback(@RequestParam String code) {
		return new RedirectView(provider.callback(Token.builder()
				.token(code)
				.build()).getUrl(fafnirConf));
	}
}
