package dk.acto.fafnir.providers;

import dk.acto.fafnir.FailureReason;
import dk.acto.fafnir.TokenFactory;
import dk.acto.fafnir.model.CallbackResult;
import dk.acto.fafnir.model.FafnirUser;
import dk.acto.fafnir.model.conf.FafnirConf;
import dk.acto.fafnir.model.conf.TestConf;
import dk.acto.fafnir.providers.credentials.Token;
import lombok.AllArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@ConditionalOnBean(TestConf.class)
public class TestProvider implements RedirectingAuthenticationProvider<Token> {
	private final TokenFactory tokenFactory;
	private final FafnirConf fafnirConf;

	@Override
	public String authenticate() {
		String jwt = tokenFactory.generateToken(
				FafnirUser.builder()
						.subject("test")
						.provider("test")
						.name("TEsty McTestface")
						.build());
		return fafnirConf.getSuccessRedirect() + "#" + jwt;
	}

	@Override
	public CallbackResult callback(Token data) {
		return CallbackResult.failure(FailureReason.CONNECTION_FAILED);
	}
}
