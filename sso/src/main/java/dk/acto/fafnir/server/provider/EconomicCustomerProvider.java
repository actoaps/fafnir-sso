package dk.acto.fafnir.server.provider;

import com.google.common.net.UrlEscapers;
import dk.acto.fafnir.api.model.*;
import dk.acto.fafnir.api.service.AdministrationService;
import dk.acto.fafnir.server.model.FailureReason;
import dk.acto.fafnir.server.util.TokenFactory;
import dk.acto.fafnir.server.model.CallbackResult;
import dk.acto.fafnir.server.model.conf.EconomicConf;
import dk.acto.fafnir.server.provider.credentials.UsernamePasswordCredentials;
import dk.acto.fafnir.server.provider.economic.EconomicCustomer;
import io.vavr.control.Try;
import lombok.AllArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
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

    public CallbackResult callback(final UsernamePasswordCredentials data) {
        var email = data.getUsername();
        var customerNumber = data.getPassword();

        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("X-AppSecretToken", economicConf.getAppSecretToken());
        headers.add("X-AgreementGrantToken", economicConf.getAgreementGrantToken());

        var orgActual = administrationService.readOrganisation(getMetaData());

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
                        ClaimData.empty(x.getCustomerNumber(), orgActual.getOrganisationId()),
                        getMetaData()))
                .map(CallbackResult::success)
                .recoverWith(Throwable.class, Try.of(() -> CallbackResult.failure(FailureReason.CONNECTION_FAILED)))
                .getOrElse(CallbackResult.failure(FailureReason.AUTHENTICATION_FAILED));
    }

    @Override
    public String providerId() {
        return "economic";
    }

    @Override
    public ProviderMetaData getMetaData() {
        return ProviderMetaData.builder()
                .providerId(providerId())
                .providerName("Economic Customer")
                .organisationSupport(OrganisationSupport.SINGLE)
                .inputs(List.of("App Secret Token", "Argeement Grant Token"))
                .build();
    }
}
