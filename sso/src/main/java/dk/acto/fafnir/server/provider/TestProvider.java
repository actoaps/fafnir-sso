package dk.acto.fafnir.server.provider;

import dk.acto.fafnir.api.model.UserData;
import dk.acto.fafnir.server.FailureReason;
import dk.acto.fafnir.server.TokenFactory;
import dk.acto.fafnir.server.model.CallbackResult;
import dk.acto.fafnir.api.model.FafnirUser;
import dk.acto.fafnir.server.model.conf.FafnirConf;
import dk.acto.fafnir.server.provider.credentials.TokenCredentials;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@Lazy
public class TestProvider implements RedirectingAuthenticationProvider<TokenCredentials> {
    private final TokenFactory tokenFactory;
    private final FafnirConf fafnirConf;

    @Override
    public String authenticate() {
        String jwt = tokenFactory.generateToken(
                FafnirUser.builder()
                        .data(UserData.builder()
                                .subject("test")
                                .provider("test")
                                .name("Testy McTestface")
                                .build())
                        .build());
        return fafnirConf.getSuccessRedirect() + "#" + jwt;
    }

    @Override
    public CallbackResult callback(TokenCredentials data) {
        return CallbackResult.failure(FailureReason.CONNECTION_FAILED);
    }

    @Override
    public boolean supportsOrganisationUrls() {
        return true;
    }
}
