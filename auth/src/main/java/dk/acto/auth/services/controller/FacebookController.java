package dk.acto.auth.services.controller;

import dk.acto.auth.providers.FacebookProvider;
import dk.acto.auth.providers.credentials.Token;
import dk.acto.auth.services.ServiceHelper;
import io.vavr.control.Try;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
public class FacebookController {
	private final FacebookProvider provider;

	@GetMapping
	public void authenticate(HttpServletResponse response) {
		Try.of(() -> ServiceHelper.functionalRedirectTo(response, provider::authenticate));
	}

	@GetMapping("callback")
	public void callback(HttpServletResponse response, @RequestParam String code) {
		Try.of(() -> ServiceHelper.functionalRedirectTo(response, () -> provider.callback(Token.builder()
				.token(code)
				.build())));
	}
}
