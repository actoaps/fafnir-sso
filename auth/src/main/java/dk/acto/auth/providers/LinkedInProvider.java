package dk.acto.auth.providers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.scribejava.apis.LinkedInApi20;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;
import dk.acto.auth.ActoConf;
import dk.acto.auth.TokenFactory;
import io.vavr.control.Option;
import io.vavr.control.Try;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Log4j2
@Component
public class LinkedInProvider implements Provider {
	private final ActoConf actoConf;
	private final OAuth20Service linkedInService;
	private final TokenFactory tokenFactory;
	private final ObjectMapper objectMapper;
	
	@Autowired
	public LinkedInProvider(ActoConf actoConf, TokenFactory tokenFactory, ObjectMapper objectMapper) {
		this.actoConf = actoConf;
		this.linkedInService = Try.of(() -> new ServiceBuilder(actoConf.getLinkedInAppId())
				.apiSecret(actoConf.getLinkedInSecret())
				.state(UUID.randomUUID().toString())
				.callback(actoConf.getMyUrl() + "/linkedin/callback")
				.scope("r_liteprofile r_emailaddress") //r_fullprofile
				.build(LinkedInApi20.instance())).getOrNull();
		this.tokenFactory = tokenFactory;
		this.objectMapper = objectMapper;
	}
	
	public String authenticate() {
		return linkedInService.getAuthorizationUrl();
	}
	
	public String callback(String code) {
		OAuth2AccessToken token = Option.of(code)
				.toTry()
				.mapTry(linkedInService::getAccessToken)
				.onFailure(x -> log.error("Authentication failed", x))
				.getOrNull();
		if (token == null) {
			return actoConf.getFailureUrl();
		}
		
		// Url not working try:
		//  https://api.linkedin.com/v2/me?projection=(id,firstName,lastName,profilePicture(displayImage~:playableStreams))
		final OAuthRequest linkedInRequest = new OAuthRequest(Verb.GET, "https://api.linkedin.com/v2/me");
		linkedInService.signRequest(token, linkedInRequest);
		var result = Try.of(() -> linkedInService.execute(linkedInRequest).getBody())
				.mapTry(objectMapper::readTree)
				.getOrNull();
		String subject = result.get("email").asText();
		String name = result.get("name").asText();
		if (subject == null || subject.isEmpty()) {
			return actoConf.getFailureUrl();
		}
		
		String jwt = tokenFactory.generateToken(subject, "linkedin", name);
		return actoConf.getSuccessUrl() + "#" + jwt;
	}
}

