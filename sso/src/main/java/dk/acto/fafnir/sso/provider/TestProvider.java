package dk.acto.fafnir.sso.provider;

import dk.acto.fafnir.api.model.*;
import dk.acto.fafnir.sso.model.FailureReason;
import dk.acto.fafnir.sso.util.TokenFactory;
import dk.acto.fafnir.sso.model.CallbackResult;
import dk.acto.fafnir.sso.model.conf.FafnirConf;
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
    public CallbackResult callback(TokenCredentials data) {
        return CallbackResult.failure(FailureReason.CONNECTION_FAILED);
    }

    @Override
    public String providerId() {
        return "test";
    }

    @Override
    public ProviderMetaData getMetaData() {
        return ProviderMetaData.builder()
                .providerId(providerId())
                .providerName("Test Provider (Do not use in production)")
                .inputs(List.of())
                .organisationSupport(OrganisationSupport.FAFNIR)
                .build();
    }
}
