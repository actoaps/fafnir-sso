package dk.acto.fafnir.server.providers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;
import dk.acto.fafnir.server.FailureReason;
import dk.acto.fafnir.server.TokenFactory;
import dk.acto.fafnir.server.model.CallbackResult;
import dk.acto.fafnir.api.model.FafnirUser;
import dk.acto.fafnir.server.providers.credentials.TokenCredentials;
import io.vavr.control.Option;
import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Lazy
public class LinkedInProvider implements RedirectingAuthenticationProvider<TokenCredentials> {
	private final OAuth20Service linkedInOAuth;
	private final TokenFactory tokenFactory;
	private final ObjectMapper objectMapper;

	public LinkedInProvider(@Qualifier("linkedInOAuth") OAuth20Service linkedInOAuth, TokenFactory tokenFactory, ObjectMapper objectMapper) {
		this.linkedInOAuth = linkedInOAuth;
		this.tokenFactory = tokenFactory;
		this.objectMapper = objectMapper;
	}


	public String authenticate() {
		return linkedInOAuth.getAuthorizationUrl();
	}

	public CallbackResult callback(TokenCredentials data) {
		var code = data.getToken();
		OAuth2AccessToken token = Option.of(code)
				.toTry()
				.mapTry(linkedInOAuth::getAccessToken)
				.onFailure(x -> log.error("Authentication failed", x))
				.getOrNull();
		if (token == null) {
			return CallbackResult.failure(FailureReason.AUTHENTICATION_FAILED);
		}

		// Url not working try:
		//  https://api.linkedin.com/v2/me?projection=(id,firstName,lastName,profilePicture(displayImage~:playableStreams))
		final OAuthRequest linkedInRequest = new OAuthRequest(Verb.GET, "https://api.linkedin.com/v2/me");
		linkedInOAuth.signRequest(token, linkedInRequest);
		var result = Try.of(() -> linkedInOAuth.execute(linkedInRequest).getBody())
				.mapTry(objectMapper::readTree)
				.getOrNull();
		String subject = result.get("email").asText();
		String name = result.get("name").asText();
		if (subject == null || subject.isEmpty()) {
			return CallbackResult.failure(FailureReason.AUTHENTICATION_FAILED);
		}

		String jwt = tokenFactory.generateToken(FafnirUser.builder()
				.subject(subject)
				.provider("linkedin")
				.name(name)
				.build());
		return CallbackResult.success(jwt);
	}
}
