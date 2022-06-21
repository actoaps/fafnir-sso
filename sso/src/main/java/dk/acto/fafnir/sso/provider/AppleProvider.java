package dk.acto.fafnir.sso.provider;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.github.scribejava.core.oauth.OAuth20Service;
import dk.acto.fafnir.api.model.*;
import dk.acto.fafnir.api.service.AdministrationService;
import dk.acto.fafnir.sso.util.TokenFactory;
import dk.acto.fafnir.sso.model.CallbackResult;
import dk.acto.fafnir.sso.provider.credentials.TokenCredentials;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@AllArgsConstructor
@ConditionalOnBean(name = "appleOAuth")
public class AppleProvider implements RedirectingAuthenticationProvider<TokenCredentials> {
    @Qualifier("appleOAuth")
    private final OAuth20Service appleOauth;
    private final TokenFactory tokenFactory;
    private final AdministrationService administrationService;

    public String authenticate() {
        return appleOauth.getAuthorizationUrl(Map.of("response_mode", "form_post"));
    }

    @Override
    public CallbackResult callback(TokenCredentials data) {
        var code = data.getCode();

        DecodedJWT jwtToken = JWT.decode(code);
        String subject = jwtToken.getClaims().get("sub").asString();
        String displayName = jwtToken.getClaims().get("email").asString();

        var subjectActual = UserData.builder()
                .subject(subject)
                .name(displayName)
                .build();
        var orgActual = OrganisationData.DEFAULT;
        var claimsActual = ClaimData.empty();

        String jwt = tokenFactory.generateToken(subjectActual, orgActual, claimsActual, getMetaData());

        return CallbackResult.success(jwt);
    }

    @Override
    public String providerId() {
        return "apple";
    }

    @Override
    public ProviderMetaData getMetaData() {
        return ProviderMetaData.builder()
                .inputs(List.of())
                .organisationSupport(OrganisationSupport.NONE)
                .providerName("Apple")
                .providerId(providerId())
                .build();
    }
}
