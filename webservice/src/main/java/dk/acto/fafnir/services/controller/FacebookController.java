package dk.acto.fafnir.services.controller;

import dk.acto.fafnir.model.conf.FafnirConf;
import dk.acto.fafnir.providers.FacebookProvider;
import dk.acto.fafnir.providers.credentials.TokenCredentials;
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
@RequestMapping("facebook")
@AllArgsConstructor
@ConditionalOnBean(FacebookProvider.class)
public class FacebookController {
	private final FacebookProvider provider;
	private final FafnirConf fafnirConf;

	@GetMapping
	public RedirectView authenticate(HttpServletResponse response) {
		return new RedirectView(provider.authenticate());
	}

	@GetMapping("callback")
	public RedirectView callback(HttpServletResponse response, @RequestParam String code) {
		return new RedirectView(provider.callback(TokenCredentials.builder()
				.token(code)
				.build()).getUrl(fafnirConf));
	}
}
