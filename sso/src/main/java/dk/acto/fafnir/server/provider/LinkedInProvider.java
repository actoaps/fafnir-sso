package dk.acto.fafnir.server.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;
import dk.acto.fafnir.api.model.UserData;
import dk.acto.fafnir.server.FailureReason;
import dk.acto.fafnir.server.TokenFactory;
import dk.acto.fafnir.server.model.CallbackResult;
import dk.acto.fafnir.api.model.FafnirUser;
import dk.acto.fafnir.server.provider.credentials.TokenCredentials;
import io.vavr.control.Option;
import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@ConditionalOnBean(name = "linkedInOAuth")
public class LinkedInProvider implements RedirectingAuthenticationProvider<TokenCredentials> {
    private final OAuth20Service linkedInOAuth;
    private final TokenFactory tokenFactory;
    private final ObjectMapper objectMapper;

    public LinkedInProvider(@Qualifier("linkedInOAuth") OAuth20Service linkedInOAuth, TokenFactory tokenFactory, ObjectMapper objectMapper) {
        this.linkedInOAuth = linkedInOAuth;
        this.tokenFactory = tokenFactory;
        this.objectMapper = objectMapper;
    }


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

        String jwt = tokenFactory.generateToken(FafnirUser.builder()
                .data(UserData.builder()
                        .subject(subject)
                        .provider("linkedin")
                        .name(String.format("%s %s", firstName, lastName))
                        .build())
                .build());
        return CallbackResult.success(jwt);
    }

    @Override
    public boolean supportsOrganisationUrls() {
        return false;
    }

    @Override
    public String entryPoint() {
        return "linkedin";
    }
}
