package dk.acto.fafnir.services.controller;

import dk.acto.fafnir.TokenFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequestMapping("public-key")
public class PublicKeyController {
	private final TokenFactory tokenFactory;

	@Autowired
	public PublicKeyController(TokenFactory tokenFactory) {
		this.tokenFactory = tokenFactory;
	}

	@GetMapping(produces = MediaType.TEXT_PLAIN_VALUE)
	public String getPublicKey() {
		return tokenFactory.getPublicKey();
	}
}
