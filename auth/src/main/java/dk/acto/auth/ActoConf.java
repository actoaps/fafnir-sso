package dk.acto.auth;

import dk.acto.auth.providers.validators.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.URL;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotBlank;

@Data
@Builder
@Slf4j
@AllArgsConstructor
public class ActoConf {
	@SuppressWarnings("squid:S1192")
	public static final ActoConf DEFAULT =
			ActoConf.builder()
					.facebookAppId("0")
					.facebookSecret("secret")
					.googleAppId("0")
					.googleSecret("secret")
					.linkedInAppId("0")
					.linkedInSecret("secret")
					.uniLoginAppId("0")
					.uniLoginSecret("secret")
					.uniLoginWSUsername("username")
					.uniLoginWSPassword("password")
					.uniLoginSingleSignOn(false)
					.failureUrl("http://localhost:8080/fail")
					.successUrl("http://localhost:8080/success")
					.myUrl("http://localhost:8080")
					.enableParameter(false)
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

	@NotBlank(groups = LinkedInValidator.class)
	private final String linkedInAppId;
	@NotBlank(groups = LinkedInValidator.class)
	private final String linkedInSecret;

	@NotBlank(groups = UniLoginValidator.class)
	private final String uniLoginAppId;
	@NotBlank(groups = UniLoginValidator.class)
	private final String uniLoginSecret;
	@NotBlank(groups = UniLoginValidator.class)
	private final String uniLoginWSUsername;
	@NotBlank(groups = UniLoginValidator.class)
	private final String uniLoginWSPassword;
	private final boolean uniLoginSingleSignOn;

	@NotBlank
	@URL
	private final String successUrl;
	@NotBlank
	@URL
	private final String failureUrl;
	@NotBlank
	@URL
	private final String myUrl;

	private final boolean enableParameter;

	@AssertTrue(groups = TestValidator.class)
	private final boolean testMode;
}
