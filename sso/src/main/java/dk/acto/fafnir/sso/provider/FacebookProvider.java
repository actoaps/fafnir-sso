package dk.acto.fafnir.sso.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;
import dk.acto.fafnir.api.model.*;
import dk.acto.fafnir.api.provider.RedirectingAuthenticationProvider;
import dk.acto.fafnir.api.provider.metadata.MetadataProvider;
import dk.acto.fafnir.api.service.AdministrationService;
import dk.acto.fafnir.sso.provider.credentials.TokenCredentials;
import dk.acto.fafnir.sso.util.TokenFactory;
import io.vavr.control.Option;
import io.vavr.control.Try;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class FacebookProvider implements RedirectingAuthenticationProvider<TokenCredentials> {
    private final TokenFactory tokenFactory;
    private final ObjectMapper objectMapper;
    private final OAuth20Service facebookOauth;
    private final AdministrationService administrationService;

    public String authenticate() {
        return facebookOauth.getAuthorizationUrl();
    }

    public AuthenticationResult callback(TokenCredentials data) {
        var code = data.getCode();
        OAuth2AccessToken token = Option.of(code)
                .toTry()
                .mapTry(facebookOauth::getAccessToken)
                .onFailure(x -> log.error("Authentication failed", x))
                .getOrNull();
        if (token == null) {
            return AuthenticationResult.failure(FailureReason.AUTHENTICATION_FAILED);
        }

        final OAuthRequest facebookRequest = new OAuthRequest(Verb.GET, "https://graph.facebook.com/v3.0/me?fields=email,name,id");
        facebookOauth.signRequest(token, facebookRequest);
        var result = Try.of(() -> facebookOauth.execute(facebookRequest).getBody())
                .mapTry(objectMapper::readTree)
                .getOrNull();
        var subject = result.get("email").asText();
        var displayName = result.get("name").asText();
        var id = result.get("id").asText();
        if (subject == null || subject.isEmpty()) {
            return AuthenticationResult.failure(FailureReason.AUTHENTICATION_FAILED);
        }

        var subjectActual = UserData.builder()
                .subject(subject)
                .name(displayName)
                .build();
        var orgActual = administrationService.readOrganisation(test -> test.getProviderId().equals(getMetaData().getProviderId()));
        var claimsActual = ClaimData.empty();

        String jwt = tokenFactory.generateToken(subjectActual, orgActual, claimsActual, getMetaData());
        return AuthenticationResult.success(jwt);
    }

    @Override
    public ProviderMetaData getMetaData() {
        return MetadataProvider.FACEBOOK;
    }
}
