package dk.acto.fafnir.sso.provider;

import dk.acto.fafnir.api.model.*;
import dk.acto.fafnir.api.provider.RedirectingAuthenticationProvider;
import dk.acto.fafnir.api.model.FailureReason;
import dk.acto.fafnir.api.provider.metadata.MetadataProvider;
import dk.acto.fafnir.sso.util.TokenFactory;
import dk.acto.fafnir.api.model.AuthenticationResult;
import dk.acto.fafnir.api.model.conf.FafnirConf;
import dk.acto.fafnir.sso.model.conf.TestConf;
import dk.acto.fafnir.sso.provider.credentials.TokenCredentials;
import lombok.AllArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@AllArgsConstructor
@ConditionalOnBean(TestConf.class)
public class TestProvider implements RedirectingAuthenticationProvider<TokenCredentials> {
    private final TokenFactory tokenFactory;
    private final FafnirConf fafnirConf;

    @Override
    public String authenticate() {
        String jwt = tokenFactory.generateToken(UserData.builder()
                                .subject("test")
                                .name("Testy McTestface")
                                .build(),
                OrganisationData.DEFAULT,
                ClaimData.empty(),
                getMetaData());
        return fafnirConf.getSuccessRedirect() + "#" + jwt;
    }

    @Override
    public AuthenticationResult callback(TokenCredentials data) {
        return AuthenticationResult.failure(FailureReason.CONNECTION_FAILED);
    }

    @Override
    public ProviderMetaData getMetaData() {
        return MetadataProvider.TEST;
    }
}
