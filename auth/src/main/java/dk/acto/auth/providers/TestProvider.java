package dk.acto.auth.providers;

import dk.acto.auth.ActoConf;
import dk.acto.auth.TokenFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class TestProvider implements Provider {
	private final TokenFactory tokenFactory;
	private final ActoConf actoConf;
	
	@Autowired
	public TestProvider(TokenFactory tokenFactory, ActoConf actoConf) {
		this.tokenFactory = tokenFactory;
		this.actoConf = actoConf;
	}
	
	@Override
	public String authenticate() {
		String jwt = tokenFactory.generateToken("test", "test", "Testy McTestface");
		return actoConf.getSuccessUrl() + "#" + jwt;
	}
}
