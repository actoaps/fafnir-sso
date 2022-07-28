package dk.acto.fafnir.sso.service.controller;

import dk.acto.fafnir.client.providers.PublicKeyProvider;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@AllArgsConstructor
@RestController
@Slf4j
@RequestMapping("public-key")
public class PublicKeyController {
	PublicKeyProvider publicKeyProvider;

	@GetMapping(produces = MediaType.TEXT_PLAIN_VALUE)
	public String getPublicKey() {
		return publicKeyProvider.getPublicKey();
	}
}
