package dk.acto.fafnir.sso.service.controller;

import com.google.common.io.BaseEncoding;
import dk.acto.fafnir.api.crypto.RsaKeyManager;
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
	RsaKeyManager rsaKeyManager;

	@GetMapping(produces = MediaType.TEXT_PLAIN_VALUE)
	public String getPublicKey() {
		return BaseEncoding.base64().omitPadding().encode(
				rsaKeyManager.getPublicKey().getEncoded()
		);
	}
}
