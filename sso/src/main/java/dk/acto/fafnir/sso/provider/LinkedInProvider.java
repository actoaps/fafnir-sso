package dk.acto.fafnir.sso.provider;

import com.fasterxml.jackson.databind.JsonNode;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@ConditionalOnBean(name = "linkedInOAuth")
@AllArgsConstructor
public class LinkedInProvider implements RedirectingAuthenticationProvider<TokenCredentials> {
    @Qualifier("linkedInOAuth")
    private final OAuth20Service linkedInOAuth;
    private final TokenFactory tokenFactory;
    private final ObjectMapper objectMapper;
    private final AdministrationService administrationService;

    public String authenticate() {
        return linkedInOAuth.getAuthorizationUrl();
    }

    public CallbackResult callback(TokenCredentials data) {
        var code = data.getCode();
        OAuth2AccessToken token = Option.of(code)
                .toTry()
                .mapTry(linkedInOAuth::getAccessToken)
                .onFailure(x -> log.error("Authentication failed", x))
                .getOrNull();
        if (token == null) {
            return CallbackResult.failure(FailureReason.AUTHENTICATION_FAILED);
        }

        final OAuthRequest profileRequest = new OAuthRequest(Verb.GET, "https://api.linkedin.com/v2/me");
        final OAuthRequest emailRequest = new OAuthRequest(Verb.GET, "https://api.linkedin.com/v2/emailAddress?q=members&projection=(elements*(handle~))");
        linkedInOAuth.signRequest(token, profileRequest);
        linkedInOAuth.signRequest(token, emailRequest);
        var profileResult = Try.of(() -> linkedInOAuth.execute(profileRequest).getBody())
                .mapTry(objectMapper::readTree)
                .getOrNull();
        var emailResult = Try.of(() -> linkedInOAuth.execute(emailRequest).getBody())
                .mapTry(objectMapper::readTree)
                .getOrNull();
        String firstName = profileResult.get("localizedFirstName").asText();
        String lastName = profileResult.get("localizedLastName").asText();
        String subject = Optional.ofNullable(emailResult.get("elements"))
                .map(x -> x.get(0))
                .map(x -> x.get("handle~"))
                .map(x -> x.get("emailAddress"))
                .map(JsonNode::asText)
                .orElse(null);

        if (subject == null || subject.isEmpty()) {
            return CallbackResult.failure(FailureReason.AUTHENTICATION_FAILED);
        }

        var subjectActual = UserData.builder()
                .subject(subject)
                .name(String.format("%s %s", firstName, lastName))
                .build();
        var orgActual = administrationService.readOrganisation(getMetaData());
        var claimsActual = ClaimData.empty();

        String jwt = tokenFactory.generateToken(subjectActual, orgActual, claimsActual, getMetaData());

        return CallbackResult.success(jwt);
    }

    @Override
    public boolean supportsOrganisationUrls() {
        return false;
    }

    @Override
    public String providerId() {
        return "linkedin";
    }

    @Override
    public ProviderMetaData getMetaData() {
        return ProviderMetaData.builder()
                .providerId(providerId())
                .providerName("LinkedIn")
                .organisationSupport(OrganisationSupport.SINGLE)
                .inputs(List.of())
                .build();
    }
}
