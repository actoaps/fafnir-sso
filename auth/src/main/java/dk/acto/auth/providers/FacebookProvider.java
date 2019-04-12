package dk.acto.auth.providers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.scribejava.apis.FacebookApi;
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
public class FacebookProvider implements Provider {
	private final ActoConf actoConf;
	private final OAuth20Service facebookService;
	private final TokenFactory tokenFactory;
	private final ObjectMapper objectMapper;
	
	@Autowired
	public FacebookProvider(ActoConf actoConf, TokenFactory tokenFactory, ObjectMapper objectMapper) {
		this.actoConf = actoConf;
		this.facebookService = Try.of(() -> new ServiceBuilder(actoConf.getFacebookAppId())
				.apiSecret(actoConf.getFacebookSecret())
				.state(UUID.randomUUID().toString())
				.callback(actoConf.getMyUrl() + "/facebook/callback")
				.scope("email")
				.build(FacebookApi.instance())).getOrNull();
		this.tokenFactory = tokenFactory;
		this.objectMapper = objectMapper;
	}
	
	public String authenticate() {
		return facebookService.getAuthorizationUrl();
	}
	
	public String callback(String code) {
		OAuth2AccessToken token = Option.of(code)
				.toTry()
				.mapTry(facebookService::getAccessToken)
				.onFailure(x -> log.error("Authentication failed", x))
				.getOrNull();
		if (token == null) {
			return actoConf.getFailureUrl();
		}
		
		final OAuthRequest facebookRequest = new OAuthRequest(Verb.GET, "https://graph.facebook.com/v3.0/me?fields=email,name");
		facebookService.signRequest(token, facebookRequest);
		var result = Try.of(() -> facebookService.execute(facebookRequest).getBody())
				.mapTry(objectMapper::readTree)
				.getOrNull();
		String subject = result.get("email").asText();
		String name = result.get("name").asText();
		if (subject == null || subject.isEmpty()) {
			return actoConf.getFailureUrl();
		}
		
		String jwt = tokenFactory.generateToken(subject, "facebook", name);
		return actoConf.getSuccessUrl() + "#" + jwt;
	}
}

