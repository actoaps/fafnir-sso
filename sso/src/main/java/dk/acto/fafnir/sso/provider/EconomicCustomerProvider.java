package dk.acto.fafnir.sso.provider;

import com.google.common.net.UrlEscapers;
import dk.acto.fafnir.api.model.*;
import dk.acto.fafnir.api.provider.RedirectingAuthenticationProvider;
import dk.acto.fafnir.api.provider.metadata.MetadataProvider;
import dk.acto.fafnir.api.service.AdministrationService;
import dk.acto.fafnir.sso.model.conf.EconomicConf;
import dk.acto.fafnir.sso.provider.credentials.UsernamePasswordCredentials;
import dk.acto.fafnir.sso.provider.economic.EconomicCustomer;
import dk.acto.fafnir.sso.util.TokenFactory;
import io.vavr.control.Try;
import lombok.AllArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Locale;
import java.util.Map;

@Component
@AllArgsConstructor
@ConditionalOnBean(EconomicConf.class)
public class EconomicCustomerProvider implements RedirectingAuthenticationProvider<UsernamePasswordCredentials> {
    private final TokenFactory tokenFactory;
    private final RestTemplate restTemplate = new RestTemplate();
    private final EconomicConf economicConf;
    private final AdministrationService administrationService;
    private final Map<String, Locale> localeMap = Map.of(
            "NOK", Locale.forLanguageTag("no-NO"),
            "SEK", Locale.forLanguageTag("sv-SE"),
            "EUR", Locale.forLanguageTag("en-GB")
    );

    @Override
    public String authenticate() {
        return "/economic/login";
    }

    public AuthenticationResult callback(final UsernamePasswordCredentials data) {
        var email = data.getUsername();
        var customerNumber = data.getPassword();

        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("X-AppSecretToken", economicConf.getAppSecretToken());
        headers.add("X-AgreementGrantToken", economicConf.getAgreementGrantToken());

        var orgActual = administrationService.readOrganisation(test -> test.getProviderId().equals(getMetaData().getProviderId()));

        return Try.of(() -> "https://restapi.e-conomic.com/customers/" + UrlEscapers.urlPathSegmentEscaper().escape(customerNumber))
                .map(x -> restTemplate.exchange(x, HttpMethod.GET, new HttpEntity<>(headers), EconomicCustomer.class))
                .map(HttpEntity::getBody)
                .filter(x -> x.getEmail() != null)
                .filter(x -> x.getEmail().equals(email))
                .map(x -> tokenFactory.generateToken(UserData.builder()
                                .subject(x.getCustomerNumber())
                                .name(x.getName())
                                .locale(localeMap.getOrDefault(x.getCurrency(), Locale.forLanguageTag("da-DK")))
                                .build(),
                        orgActual,
                        ClaimData.empty(),
                        getMetaData()))
                .map(AuthenticationResult::success)
                .recoverWith(Throwable.class, Try.of(() -> AuthenticationResult.failure(FailureReason.CONNECTION_FAILED)))
                .getOrElse(AuthenticationResult.failure(FailureReason.AUTHENTICATION_FAILED));
    }

    @Override
    public ProviderMetaData getMetaData() {
        return MetadataProvider.ECONOMIC;
    }
}
