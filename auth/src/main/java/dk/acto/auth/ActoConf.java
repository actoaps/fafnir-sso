package dk.acto.auth;

import dk.acto.auth.providers.FacebookProvider;
import dk.acto.auth.providers.GoogleProvider;
import dk.acto.auth.providers.UniLoginProvider;
import lombok.Builder;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

import javax.validation.constraints.NotBlank;

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

    @NotBlank(groups = FacebookProvider.class)
    private final String facebookAppId;
    @NotBlank(groups = FacebookProvider.class)
    private final String facebookSecret;

    @NotBlank(groups = GoogleProvider.class)
    private final String googleAppId;
    @NotBlank(groups = GoogleProvider.class)
    private final String googleSecret;

    @NotBlank(groups = UniLoginProvider.class)
    private final String uniLoginAppId;
    @NotBlank(groups = UniLoginProvider.class)
    private final String uniLoginSecret;
    @NotBlank(groups = UniLoginProvider.class)
    private final String uniLoginWSUsername;
    @NotBlank(groups = UniLoginProvider.class)
    private final String uniLoginWSPassword;
    @NotBlank
    @URL
    private final String successUrl;
    @NotBlank
    @URL
    private final String failureUrl;
    @NotBlank
    @URL
    private final String myUrl;

    private final boolean emitTestToken;
}
