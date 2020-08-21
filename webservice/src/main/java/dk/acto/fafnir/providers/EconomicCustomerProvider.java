package dk.acto.fafnir.providers;

import com.google.common.net.UrlEscapers;
import dk.acto.fafnir.FailureReason;
import dk.acto.fafnir.TokenFactory;
import dk.acto.fafnir.model.CallbackResult;
import dk.acto.fafnir.model.FafnirUser;
import dk.acto.fafnir.model.conf.EconomicConf;
import dk.acto.fafnir.providers.credentials.UsernamePasswordCredentials;
import dk.acto.fafnir.providers.economic.EconomicCustomer;
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
    private final HttpHeaders httpHeaders;
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

        return Try.of(() -> "https://restapi.e-conomic.com/customers/"  + UrlEscapers.urlPathSegmentEscaper().escape(customerNumber))
                        .map(x -> restTemplate.exchange(x, HttpMethod.GET, new HttpEntity<>(httpHeaders), EconomicCustomer.class))
                        .map(HttpEntity::getBody)
                .filter(x -> x.getEmail() != null)
                .filter(x -> x.getEmail().equals(email))
                .map(x -> tokenFactory.generateToken(FafnirUser.builder()
                        .subject(x.getCustomerNumber())
                        .provider("economic")
                        .name(x.getName())
                        .locale(localeMap.getOrDefault(x.getCurrency(), Locale.forLanguageTag("da-DK")))
                                .build()))
                .map(CallbackResult::success)
                .recoverWith(Throwable.class,Try.of(() -> CallbackResult.failure(FailureReason.CONNECTION_FAILED)))
                .getOrElse(CallbackResult.failure(FailureReason.AUTHENTICATION_FAILED));
    }

    private HttpHeaders getHeaders (EconomicConf economicConf) {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("X-AppSecretToken", economicConf.getAppSecretToken());
        headers.add("X-AgreementGrantToken", economicConf.getAgreementGrantToken());
        return headers;
    }
}
