package dk.acto.auth.providers;

import dk.acto.auth.ActoConf;
import dk.acto.auth.TokenFactory;
import dk.acto.auth.providers.economic.EconomicCustomer;
import dk.acto.auth.services.ServiceHelper;
import io.vavr.control.Option;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
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
            "DKK", "da-DK"
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
        var result = Option.of("https://restapi.e-conomic.com/customers")
                        .map(x -> restTemplate.exchange(x, HttpMethod.GET, new HttpEntity<>(httpHeaders), EconomicCustomer.CustomerWrapper.class))
                        .map(HttpEntity::getBody)
                        .map(EconomicCustomer.CustomerWrapper::getCollection)
                        .getOrElse(List.of()).stream()
                .filter(x -> x.getCustomerNumber().equals(customerNumber) && x.getEmail().equals(email))
                .map(x -> tokenFactory.generateToken(x.getCustomerNumber(),
                        "economic",
                        x.getName(),
                        localeMap.getOrDefault(x.getCurrency(), "en-GB")))
                .findAny();
        return ServiceHelper.getJwtUrl(actoConf, result.orElse(null));
    }

    private HttpHeaders getHeaders (ActoConf actoConf) {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("X-AppSecretToken", actoConf.getEconomicAppSecretToken());
        headers.add("X-AgreementGrantToken", actoConf.getEconomicAgreementGrantToken());
        return headers;
    }
}
