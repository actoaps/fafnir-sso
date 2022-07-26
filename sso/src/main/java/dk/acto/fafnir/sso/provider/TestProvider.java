package dk.acto.fafnir.sso.provider;

import dk.acto.fafnir.api.model.*;
import dk.acto.fafnir.api.model.conf.FafnirConf;
import dk.acto.fafnir.api.provider.RedirectingAuthenticationProvider;
import dk.acto.fafnir.api.provider.metadata.MetadataProvider;
import dk.acto.fafnir.sso.provider.credentials.TokenCredentials;
import dk.acto.fafnir.sso.util.TokenFactory;
import lombok.AllArgsConstructor;

@AllArgsConstructor
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
