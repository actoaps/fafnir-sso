package dk.acto.fafnir.server.service.controller;

import dk.acto.fafnir.server.model.conf.FafnirConf;
import dk.acto.fafnir.server.provider.FacebookProvider;
import dk.acto.fafnir.server.provider.credentials.TokenCredentials;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;

import javax.annotation.PostConstruct;
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
	public RedirectView authenticate() {
		return new RedirectView(provider.authenticate());
	}

	@GetMapping("callback")
	public RedirectView callback(@RequestParam String code) {
		return new RedirectView(provider.callback(TokenCredentials.builder()
				.code(code)
				.build()).getUrl(fafnirConf));
	}

	@PostConstruct
	private void postConstruct() {
		log.info("Exposing Facebook Endpoint...");
	}

}

