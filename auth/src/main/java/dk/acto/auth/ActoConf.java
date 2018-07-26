package dk.acto.auth;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ActoConf {
    public static final ActoConf DEFAULT = ActoConf.builder()
            .facebookAppId("0")
            .facebookSecret("secret")
            .googleAppId("0")
            .googleSecret("secret")
            .failureUrl("http://localhost:8080/fail")
            .successUrl("http://localhost:8080/dashboard")
            .build();

    private final String facebookAppId;
    private final String facebookSecret;
    private final String googleAppId;
    private final String googleSecret;
    private final String successUrl;
    private final String failureUrl;
    private final String myUrl;
}
