package dk.acto.fafnir.server.provider;

import com.google.common.net.UrlEscapers;
import dk.acto.fafnir.api.model.FafnirUser;
import dk.acto.fafnir.api.model.UserData;
import dk.acto.fafnir.server.FailureReason;
import dk.acto.fafnir.server.TokenFactory;
import dk.acto.fafnir.server.model.CallbackResult;
import dk.acto.fafnir.server.model.conf.EconomicConf;
import dk.acto.fafnir.server.provider.credentials.UsernamePasswordCredentials;
import dk.acto.fafnir.server.provider.economic.EconomicCustomer;
import io.vavr.control.Try;
import lombok.AllArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Lazy;
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

        return Try.of(() -> "https://restapi.e-conomic.com/customers/" + UrlEscapers.urlPathSegmentEscaper().escape(customerNumber))
                .map(x -> restTemplate.exchange(x, HttpMethod.GET, new HttpEntity<>(headers), EconomicCustomer.class))
                .map(HttpEntity::getBody)
                .filter(x -> x.getEmail() != null)
                .filter(x -> x.getEmail().equals(email))
                .map(x -> tokenFactory.generateToken(FafnirUser.builder()
                        .data(UserData.builder()
                                .subject(x.getCustomerNumber())
                                .provider("economic")
                                .name(x.getName())
                                .locale(localeMap.getOrDefault(x.getCurrency(), Locale.forLanguageTag("da-DK")))
                                .build())
                        .build()))
                .map(CallbackResult::success)
                .recoverWith(Throwable.class, Try.of(() -> CallbackResult.failure(FailureReason.CONNECTION_FAILED)))
                .getOrElse(CallbackResult.failure(FailureReason.AUTHENTICATION_FAILED));
    }

    @Override
    public boolean supportsOrganisationUrls() {
        return false;
    }

    @Override
    public String entryPoint() {
        return "economic";
    }
}
