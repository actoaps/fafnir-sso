package dk.acto.fafnir.server.service.controller;

import dk.acto.fafnir.server.model.conf.FafnirConf;
import dk.acto.fafnir.server.provider.MicrosoftIdentityProvider;
import dk.acto.fafnir.server.provider.credentials.TokenCredentials;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;

import javax.annotation.PostConstruct;

@Controller
@Slf4j
@RequestMapping("msidentity")
@AllArgsConstructor
@ConditionalOnProperty(name = {"MSID_AID", "MSID_SECRET", "MSID_TENANT"})
public class MicrosoftIdentityController {
	private final MicrosoftIdentityProvider provider;
	private final FafnirConf fafnirConf;

	@GetMapping
	public RedirectView authenticate() {
		return new RedirectView(provider.authenticate());
	}

	@GetMapping("callback")
	public RedirectView callback(@RequestParam String code) {
		return new RedirectView(provider.callback(TokenCredentials.builder()
				.token(code)
				.build()).getUrl(fafnirConf));
	}

	@PostConstruct
	private void postConstruct() {
		log.info("Exposing Microsoft Identity Endpoint...");
	}
}
