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
	private static final String SECRET = "secret";
	private static final String DEMO = "demo";
	private static final String ZERO = "0";

	public static final ActoConf DEFAULT =
			ActoConf.builder()
					.facebookAppId(ZERO)
					.facebookSecret(SECRET)
					.googleAppId(ZERO)
					.googleSecret(SECRET)
					.linkedInAppId(ZERO)
					.linkedInSecret(SECRET)
					.uniLoginAppId(ZERO)
					.uniLoginSecret(SECRET)
					.uniLoginWSUsername("username")
					.uniLoginWSPassword("password")
					.uniLoginSingleSignOn(false)
					.economicAppSecretToken(DEMO)
					.economicAgreementGrantToken(DEMO)
					.failureUrl("http://localhost:8080/fail")
					.successUrl("http://localhost:8080/success")
					.myUrl("http://localhost:8080")
					.testMode(false)
					.hazelcastUsernameIsEmail(false)
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

	@NotBlank(groups = EconomicCustomerValidator.class)
	private final String economicAppSecretToken;
	@NotBlank(groups = EconomicCustomerValidator.class)
	private final String economicAgreementGrantToken;

	@NotBlank
	@URL
	private final String successUrl;
	@NotBlank
	@URL
	private final String failureUrl;
	@NotBlank
	@URL
	private final String myUrl;

	private final boolean hazelcastUsernameIsEmail;

	@AssertTrue(groups = TestValidator.class)
	private final boolean testMode;
}
