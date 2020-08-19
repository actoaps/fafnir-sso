package dk.acto.auth.providers;

import dk.acto.auth.FailureReason;
import dk.acto.auth.TokenFactory;
import dk.acto.auth.model.CallbackResult;
import dk.acto.auth.model.FafnirUser;
import dk.acto.auth.model.conf.FafnirConf;
import dk.acto.auth.model.conf.TestConf;
import dk.acto.auth.providers.credentials.Token;
import lombok.AllArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.view.RedirectView;

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
