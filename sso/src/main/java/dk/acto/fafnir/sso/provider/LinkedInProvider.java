package dk.acto.fafnir.sso.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.util.Optional;

@Slf4j
@AllArgsConstructor
public class LinkedInProvider implements RedirectingAuthenticationProvider<TokenCredentials> {
    private final OAuth20Service linkedInOAuth;
    private final TokenFactory tokenFactory;
    private final ObjectMapper objectMapper;
    private final AdministrationService administrationService;

    public String authenticate() {
        return linkedInOAuth.getAuthorizationUrl();
    }

    public AuthenticationResult callback(TokenCredentials data) {
        var code = data.getCode();
        var token = Option.of(code)
                .toTry()
                .mapTry(linkedInOAuth::getAccessToken)
                .onFailure(x -> log.error("Authentication failed", x))
                .getOrNull();
        if (token == null) {
            return AuthenticationResult.failure(FailureReason.AUTHENTICATION_FAILED);
        }

        final var profileRequest = new OAuthRequest(Verb.GET, "https://api.linkedin.com/v2/me");
        final var emailRequest = new OAuthRequest(Verb.GET, "https://api.linkedin.com/v2/emailAddress?q=members&projection=(elements*(handle~))");
        linkedInOAuth.signRequest(token, profileRequest);
        linkedInOAuth.signRequest(token, emailRequest);
        var profileResult = Try.of(() -> linkedInOAuth.execute(profileRequest).getBody())
                .mapTry(objectMapper::readTree)
                .getOrNull();
        var emailResult = Try.of(() -> linkedInOAuth.execute(emailRequest).getBody())
                .mapTry(objectMapper::readTree)
                .getOrNull();
        var firstName = profileResult.get("localizedFirstName").asText();
        var lastName = profileResult.get("localizedLastName").asText();
        var subject = Optional.ofNullable(emailResult.get("elements"))
                .map(x -> x.get(0))
                .map(x -> x.get("handle~"))
                .map(x -> x.get("emailAddress"))
                .map(JsonNode::asText)
                .orElse(null);

        if (subject == null || subject.isEmpty()) {
            return AuthenticationResult.failure(FailureReason.AUTHENTICATION_FAILED);
        }

        var subjectActual = UserData.builder()
                .subject(subject)
                .name(String.format("%s %s", firstName, lastName))
                .build();
        var orgActual = administrationService.readOrganisation(test -> test.getProviderId().equals(getMetaData().getProviderId()));
        var claimsActual = ClaimData.empty();

        var jwt = tokenFactory.generateToken(subjectActual, orgActual, claimsActual, getMetaData());

        return AuthenticationResult.success(jwt);
    }

    @Override
    public ProviderMetaData getMetaData() {
        return MetadataProvider.LINKEDIN;
    }
}
