package dk.acto.auth;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ActoConf {
    public static ActoConf DEFAULT = ActoConf.builder()
            .facebookAppId("0")
            .facebookSecret("secret")
            .failureUrl("http://localhost:8080/fail")
            .successUrl("http://localhost:8080/dashboard")
            .jwtSecret("MIICXAIBAAKBgQCMtWwxBfzbODUc5hBUVxKZfJ8bBEksEJd+OYPUg1bhmSd4U3aO\n" +
                    "6WRU2lCnZT6sx4dnJz/ICLlv7mplp23ebjx9v1RVPcQGYLu1a9Jn1H2f1fu8JVWZ\n" +
                    "/tg2CXwPaSnLbHiQXu5+WGCIL2CuNZt0l/+A4gxR1Rf+x6MrwV8okb2UewIDAQAB\n" +
                    "AoGAHkQ0BoOENV9sxU+TeaDVJmDRFI2ic7EJ9SPIAKFTwekgvVguq2T2qO3g5XD4\n" +
                    "v0+YB49Av/VodfQxvLX8AxlxN0WXCWDLLlFLO++6k4xRCsWqXBMyuZr0dKWNvGjl\n" +
                    "KJ9NlPjfk9QBqNwrYA4/j9oMTp39SfD7gW3TH3DxiB1S6KkCQQC+RjN78GPVx3n2\n" +
                    "r5RaWgT4vXV1YKuUBstDtHOxxvys1rqPxsZdApXe52AIKu99+KyuzSZ9PZ9mdRVs\n" +
                    "cZ2j2tK9AkEAvVAuEpd8wvefvLaImxasaXi4KIrFrBoLtJNKT+/YmdsbQzR9ka+K\n" +
                    "lpEXO4U4a2X03Uw1mT0ZtlcDyg9IYDhTlwJABcfOMpq3/bukqejloeUQN5pR4jIA\n" +
                    "pGucaz8lMKZx8LJJUqrgRd0ZPn9a/ISJaBNQ87KJ0842dH9kGjpNZrf0JQJABrsF\n" +
                    "CFAudVgMa88piCoEMzPBiF92q6m5ZNAfwjvKvZ2WmIsTM8zD5pp98vXHjbnwPLXq\n" +
                    "6enMMjlhvxtP/WvDPQJBAJDwj3YSL+nlFx9K7dm0ep4XpPlpBoTA0Z+y8RuStuMI\n" +
                    "hsetQ4vvbvxpdRSkstxOmQdtRCT/JcZd8E+2z8085vc=").build();

    private final String jwtSecret;
    private final String facebookAppId;
    private final String facebookSecret;
    private final String successUrl;
    private final String failureUrl;
    private final String myUrl;
}
