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
		    .uniLoginAppId("0")
		    .uniLoginSecret("secret")
		    .uniLoginWSUsername("username")
		    .uniLoginWSPassword("password")
            .failureUrl("http://localhost:8080/fail")
            .successUrl("http://localhost:8080/success")
            .myUrl("http://localhost:8080")
            .emitTestToken(false)
            .build();

    private final String facebookAppId;
    private final String facebookSecret;
    private final String googleAppId;
    private final String googleSecret;
    private final String uniLoginAppId;
    private final String uniLoginSecret;
    private final String uniLoginWSUsername;
    private final String uniLoginWSPassword;
    private final String successUrl;
    private final String failureUrl;
    private final String myUrl;
    private final boolean emitTestToken;
}
