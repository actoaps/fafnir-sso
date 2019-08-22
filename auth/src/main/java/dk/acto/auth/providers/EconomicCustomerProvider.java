package dk.acto.auth.providers;

import com.google.common.net.UrlEscapers;
import dk.acto.auth.ActoConf;
import dk.acto.auth.TokenFactory;
import dk.acto.auth.providers.economic.EconomicCustomer;
import dk.acto.auth.services.ServiceHelper;
import io.vavr.control.Try;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
public class EconomicCustomerProvider implements Provider {
    private final TokenFactory tokenFactory;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ActoConf actoConf;
    private final HttpHeaders httpHeaders;
    private final Map<String, String> localeMap = Map.of(
            "NOK", "no-NO",
            "SEK", "sv-SE",
            "EUR", "en-GB"
    );

    public EconomicCustomerProvider(TokenFactory tokenFactory, ActoConf actoConf, ActoConf actoconf) {
        this.tokenFactory = tokenFactory;
        this.actoConf = actoConf;
        this.httpHeaders = getHeaders(actoconf);
    }

    @Override
    public String authenticate() {
        return "/economic/login";
    }

    public String callback(final String email, final String customerNumber) {
        return Try.of(() -> "https://restapi.e-conomic.com/customers/"  + UrlEscapers.urlPathSegmentEscaper().escape(customerNumber))
                        .map(x -> restTemplate.exchange(x, HttpMethod.GET, new HttpEntity<>(httpHeaders), EconomicCustomer.class))
                        .map(HttpEntity::getBody)
                .filter(x -> x.getEmail() != null)
                .filter(x -> x.getEmail().equals(email))
                .map(x -> tokenFactory.generateToken(x.getCustomerNumber(),
                        "economic",
                        x.getName(),
                        localeMap.getOrDefault(x.getCurrency(), "da-DK")))
                .map(x -> ServiceHelper.getJwtUrl(actoConf, x))
                .getOrElse(actoConf.getFailureUrl());
    }

    private HttpHeaders getHeaders (ActoConf actoConf) {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("X-AppSecretToken", actoConf.getEconomicAppSecretToken());
        headers.add("X-AgreementGrantToken", actoConf.getEconomicAgreementGrantToken());
        return headers;
    }
}
