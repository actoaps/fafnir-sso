package dk.acto.auth.providers;

import dk.acto.auth.ActoConf;
import dk.acto.auth.TokenFactory;
import dk.acto.auth.model.FafnirUser;
import dk.acto.auth.providers.credentials.Token;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@AllArgsConstructor
public class TestProvider implements RedirectingAuthenticationProvider<Token> {
	private final TokenFactory tokenFactory;
	private final ActoConf actoConf;

	@Override
	public String authenticate() {
		String jwt = tokenFactory.generateToken(
				FafnirUser.builder()
						.subject("test")
						.provider("test")
						.name("TEsty McTestface")
						.build());
		return actoConf.getSuccessUrl() + "#" + jwt;
	}

	@Override
	public String callback(Token data) {
		return null;
	}
}
