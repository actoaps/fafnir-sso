package dk.acto.auth;

import com.github.scribejava.apis.FacebookApi;
import com.github.scribejava.apis.GoogleApi20;
import com.github.scribejava.apis.LinkedInApi20;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.oauth.OAuth20Service;
import dk.acto.auth.model.conf.*;
import io.vavr.control.Try;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.validation.Validator;

@Configuration
@AllArgsConstructor
public class BeanConf {
    private final Validator validator;


    @Bean
    @ConditionalOnProperty(name = {"ECONOMIC_AST", "ECONOMIC_AGT"})
    public EconomicConf getEconomicConf(
            @Value("${ECONOMIC_AST}") String secret,
            @Value("${ECONOMIC_AGT}") String grant) {
        var bean = new EconomicConf(secret, grant);
        return validator.validate(bean).isEmpty() ? bean : null;
    }

    @Bean
    @ConditionalOnProperty(name = {"FACEBOOK_AID", "FACEBOOK_SECRET"})
    public FacebookConf getFacebookConf(
            @Value("${FACEBOOK_AID}") String appId,
            @Value("${FACEBOOK_SECRET}") String secret) {
        var bean = new FacebookConf(appId, secret);
        return validator.validate(bean).isEmpty() ? bean : null;
    }

    @Bean
    @ConditionalOnProperty(name = {"GOOGLE_AID", "GOOGLE_SECRET"})
    public GoogleConf getGoogleConf(
            @Value("${GOOGLE_AID}") String appId,
            @Value("${GOOGLE_SECRET}") String secret) {
        return new GoogleConf(appId, secret);
    }

    @Bean
    @ConditionalOnProperty(name = {"LINKED_IN_AID", "LINKED_IN_SECRET"})
    public LinkedInConf getLinkedInConf(
            @Value("${LINKED_IN_AID}") String appId,
            @Value("${LINKED_IN_SECRET}") String secret) {
        return new LinkedInConf(appId, secret);
    }

    @Bean
    @ConditionalOnProperty(name = {"UL_AID", "UL_SECRET", "UL_WS_USER", "UL_WS_PASS"})
    public UniLoginConf getUnuLoginConf(
            @Value("${UL_AID}") String appId,
            @Value("${UL_SECRET}") String secret,
            @Value("${UL_WS_USER}") String wsUser,
            @Value("${UL_WS_PASS}") String wsPass,
            @Value("${UL_SSO:false}") boolean sso) {
        return new UniLoginConf(appId, secret, wsUser, wsPass, sso);
    }

    @Bean
    public HazelcastConf getHazelcastConf(
            @Value("${HAZELCAST_USERNAME_IS_EMAIL:false}") boolean userNameIsEmail,
            @Value("${HAZELCAST_PASSWORD_IS_ENCRYPTED:false}") boolean passwordIsEncrypted) {
        return new HazelcastConf(userNameIsEmail, passwordIsEncrypted);
    }

    @Bean
    @ConditionalOnProperty(name = "TEST_ENABLED")
    public TestConf getTestConf() {
        return new TestConf(true);
    }

    @Bean
    public FafnirConf getFafnirConf(
            @Value("${FAFNIR_URL:http://localhost:8080}") String url,
            @Value("${FAFNIR_SUCCESS:http://localhost:8080/success}") String success,
            @Value("${FAFNIR_FAILURE:http://localhost:8080/fail}") String failure) {
        return new FafnirConf(url, success, failure);
    }

    @Bean
    @ConditionalOnBean(GoogleConf.class)
    public OAuth20Service getGoogleOAuth (GoogleConf googleConf, FafnirConf fafnirConf) {
        return Try.of(() -> new ServiceBuilder(googleConf.getAppId())
                .apiSecret(googleConf.getSecret())
                .callback(fafnirConf + "/google/callback")
                .defaultScope("openid email profile")
                .build(GoogleApi20.instance())).getOrNull();
    }

    @Bean
    @ConditionalOnBean(FacebookConf.class)
    public OAuth20Service getFacebookOAuth (FacebookConf facebookConf, FafnirConf fafnirConf) {
        return Try.of(() -> new ServiceBuilder(facebookConf.getAppId())
                .apiSecret(facebookConf.getSecret())
                .callback(fafnirConf.getUrl() + "/facebook/callback")
                .defaultScope("email")
                .build(FacebookApi.instance())).getOrNull();
    }

    @Bean
    @ConditionalOnBean(LinkedInConf.class)
    public OAuth20Service linkedInOAuth(LinkedInConf linkedInConf, FafnirConf fafnirConf ) {
        return Try.of(() -> new ServiceBuilder(linkedInConf.getAppId())
                .apiSecret(linkedInConf.getSecret())
                .callback(fafnirConf.getUrl() + "/linkedin/callback")
                .defaultScope("r_liteprofile r_emailaddress") //r_fullprofile
                .build(LinkedInApi20.instance())).getOrNull();
    }
}
