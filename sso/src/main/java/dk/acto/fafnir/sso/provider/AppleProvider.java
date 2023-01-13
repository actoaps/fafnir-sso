package dk.acto.fafnir.sso.provider;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.Claim;
import com.github.scribejava.core.oauth.OAuth20Service;
import dk.acto.fafnir.api.exception.ProviderAttributeMissing;
import dk.acto.fafnir.api.model.*;
import dk.acto.fafnir.api.provider.RedirectingAuthenticationProvider;
import dk.acto.fafnir.api.provider.metadata.MetadataProvider;
import dk.acto.fafnir.sso.model.conf.ProviderConf;
import dk.acto.fafnir.sso.provider.credentials.TokenCredentials;
import dk.acto.fafnir.sso.util.TokenFactory;
import io.vavr.control.Try;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Optional;

@Slf4j
@AllArgsConstructor
public class AppleProvider implements RedirectingAuthenticationProvider<TokenCredentials> {
    private final OAuth20Service appleOauth;
    private final TokenFactory tokenFactory;
    private final ProviderConf providerConf;

    public String authenticate() {
        return appleOauth.getAuthorizationUrl(Map.of("response_mode", "form_post"));
    }

    @Override
    public AuthenticationResult callback(TokenCredentials data) {
        var token = Try.of(() -> JWT.decode(data.getCode()))
                .onFailure(x -> log.error("Authentication failed", x))
                .getOrNull();
        if (token == null) {
            return AuthenticationResult.failure(FailureReason.AUTHENTICATION_FAILED);
        }

        var subject = Optional.ofNullable(token.getClaim("sub"))
                .map(Claim::asString)
                .map(providerConf::applySubjectRules)
                .orElseThrow(ProviderAttributeMissing::new);
        var displayName = Optional.ofNullable(token.getClaim("email"))
                .map(Claim::asString)
                .orElseThrow(ProviderAttributeMissing::new);

        var subjectActual = UserData.builder()
                .subject(subject)
                .name(displayName)
                .build();
        var orgActual = OrganisationData.DEFAULT;
        var claimsActual = ClaimData.empty();

        var jwt = tokenFactory.generateToken(subjectActual, orgActual, claimsActual, getMetaData());

        return AuthenticationResult.success(jwt);
    }

    @Override
    public ProviderMetaData getMetaData() {
        return MetadataProvider.APPLE;
    }
}
