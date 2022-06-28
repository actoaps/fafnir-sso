package dk.acto.fafnir.sso.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;
import dk.acto.fafnir.api.model.*;
import dk.acto.fafnir.api.service.AdministrationService;
import dk.acto.fafnir.sso.model.FailureReason;
import dk.acto.fafnir.sso.util.TokenFactory;
import dk.acto.fafnir.sso.model.CallbackResult;
import dk.acto.fafnir.sso.provider.credentials.TokenCredentials;
import io.vavr.control.Option;
import io.vavr.control.Try;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.util.List;

@Log4j2
@Component
@AllArgsConstructor
@ConditionalOnBean(name = "facebookOAuth")
public class FacebookProvider implements RedirectingAuthenticationProvider<TokenCredentials> {
    private final TokenFactory tokenFactory;
    private final ObjectMapper objectMapper;
    @Qualifier("facebookOauth")
    private final OAuth20Service facebookOauth;
    private final AdministrationService administrationService;

    public String authenticate() {
        return facebookOauth.getAuthorizationUrl();
    }

    public CallbackResult callback(TokenCredentials data) {
        var code = data.getCode();
        OAuth2AccessToken token = Option.of(code)
                .toTry()
                .mapTry(facebookOauth::getAccessToken)
                .onFailure(x -> log.error("Authentication failed", x))
                .getOrNull();
        if (token == null) {
            return CallbackResult.failure(FailureReason.AUTHENTICATION_FAILED);
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
            return CallbackResult.failure(FailureReason.AUTHENTICATION_FAILED);
        }

        var subjectActual = UserData.builder()
                .subject(subject)
                .name(displayName)
                .build();
        var orgActual = administrationService.readOrganisation(test -> test.getProviderId().equals(providerId()));
        var claimsActual = ClaimData.empty();

        String jwt = tokenFactory.generateToken(subjectActual, orgActual, claimsActual, getMetaData());
        return CallbackResult.success(jwt);
    }

    @Override
    public String providerId() {
        return "facebook";
    }

    @Override
    public ProviderMetaData getMetaData() {
        return ProviderMetaData.builder()
                .providerId(providerId())
                .providerName("Facebook")
                .inputs(List.of())
                .organisationSupport(OrganisationSupport.SINGLE)
                .build();
    }
}
