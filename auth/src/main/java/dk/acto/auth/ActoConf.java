package dk.acto.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import dk.acto.auth.providers.validators.FacebookValidator;
import dk.acto.auth.providers.validators.GoogleValidator;
import dk.acto.auth.providers.validators.TestValidator;
import dk.acto.auth.providers.validators.UniLoginValidator;
import io.vavr.control.Option;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.URL;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotBlank;
import java.util.function.Supplier;

@Data
@Builder
@Slf4j
@AllArgsConstructor
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
            .testMode(true)
            .build();

    @NotBlank(groups = FacebookValidator.class)
    private final String facebookAppId;
    @NotBlank(groups = FacebookValidator.class)
    private final String facebookSecret;

    @NotBlank(groups = GoogleValidator.class)
    private final String googleAppId;
    @NotBlank(groups = GoogleValidator.class)
    private final String googleSecret;

    @NotBlank(groups = UniLoginValidator.class)
    private final String uniLoginAppId;
    @NotBlank(groups = UniLoginValidator.class)
    private final String uniLoginSecret;
    @NotBlank(groups = UniLoginValidator.class)
    private final String uniLoginWSUsername;
    @NotBlank(groups = UniLoginValidator.class)
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

    @AssertTrue(groups = TestValidator.class)
    private final boolean testMode;
}
