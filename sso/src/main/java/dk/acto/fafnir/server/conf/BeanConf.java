package dk.acto.fafnir.server.conf;

import com.github.scribejava.apis.FacebookApi;
import com.github.scribejava.apis.GoogleApi20;
import com.github.scribejava.apis.LinkedInApi20;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.oauth.OAuth20Service;
import com.hazelcast.client.config.ClientConfig;
import dk.acto.fafnir.api.model.conf.HazelcastConf;
import dk.acto.fafnir.server.model.conf.*;
import dk.acto.fafnir.server.service.AppleApi;
import dk.acto.fafnir.server.service.MicrosoftIdentityApi;
import dk.acto.fafnir.server.service.MitIdApi;
import dk.acto.fafnir.server.util.DemoDataGenerator;
import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Slf4j
@Configuration
public class BeanConf {

    @Bean
    @ConditionalOnProperty(name = {"ECONOMIC_AST", "ECONOMIC_AGT"})
    public EconomicConf economicConf(
            @Value("${ECONOMIC_AST}") String secret,
            @Value("${ECONOMIC_AGT}") String grant) {
        log.info("Economic Customer Configured...");
        return new EconomicConf(secret, grant);
    }

    @Bean
    @ConditionalOnProperty(name = {"FACEBOOK_AID", "FACEBOOK_SECRET"})
    public FacebookConf facebookConf(
            @Value("${FACEBOOK_AID}") String appId,
            @Value("${FACEBOOK_SECRET}") String secret) {
        log.info("Facebook Configured...");
        return new FacebookConf(appId, secret);
    }

    @Bean
    @ConditionalOnProperty(name = {"GOOGLE_AID", "GOOGLE_SECRET"})
    public GoogleConf googleConf(
            @Value("${GOOGLE_AID}") String appId,
            @Value("${GOOGLE_SECRET}") String secret) {
        log.info("Google Configured...");
        return new GoogleConf(appId, secret);
    }

    @Bean
    @ConditionalOnProperty(name = {"LINKED_IN_AID", "LINKED_IN_SECRET"})
    public LinkedInConf linkedInConf(
            @Value("${LINKED_IN_AID}") String appId,
            @Value("${LINKED_IN_SECRET}") String secret) {
        log.info("LinkedIn Configured...");
        return new LinkedInConf(appId, secret);
    }

    @Bean
    @ConditionalOnProperty(name = {"UL_AID", "UL_SECRET", "UL_WS_USER", "UL_WS_PASS"})
    public UniLoginConf uniLoginConf(
            @Value("${UL_AID}") String appId,
            @Value("${UL_SECRET}") String secret,
            @Value("${UL_WS_USER}") String wsUser,
            @Value("${UL_WS_PASS}") String wsPass,
            @Value("${UL_SSO:false}") boolean sso) {
        log.info("UniLogin Configured...");
        return new UniLoginConf(appId, secret, wsUser, wsPass, sso);
    }

    @Bean
    @Primary
    public HazelcastConf hazelcastConf(
            @Value("${HAZELCAST_MAP_NAME:fafnir-users}") String mapName,
            @Value("${HAZELCAST_USERNAME_IS_EMAIL:false}") boolean userNameIsEmail,
            @Value("${HAZELCAST_PASSWORD_IS_ENCRYPTED:false}") boolean passwordIsEncrypted,
            @Value("${HAZELCAST_TRIM_USERNAME:false}") boolean trimUsername) {
        return new HazelcastConf(userNameIsEmail, passwordIsEncrypted, trimUsername, mapName);
    }

    @Bean
    @ConditionalOnProperty(name = "HAZELCAST_TCP_IP_ADDRESS")
    public ClientConfig hazelcastInstanceConf (@Value("${HAZELCAST_TCP_IP_ADDRESS}") String address) {
        log.info("Hazelcast TCP/IP Connection Configured...");
        var config = new ClientConfig();
        config.getNetworkConfig().addAddress(address);
        return config;
    }

    @Bean
    @ConditionalOnProperty(name = "TEST_ENABLED")
    public TestConf testConf(@Value("${TEST_ENABLED:false}") boolean testEnabled) {
        log.info("Test Configured...");
        return new TestConf(testEnabled);
    }

    @Bean
    @ConditionalOnProperty(name = {"APPLE_AID", "APPLE_SECRET"})
    public AppleConf appleConf(
            @Value("${APPLE_AID}") String appId,
            @Value("${APPLE_SECRET}") String secret) {
        log.info("Apple Configured...");
        return new AppleConf(appId, secret);
    }

    @Bean
    @ConditionalOnProperty(name = {"MITID_AID", "MITID_SECRET", "MITID_AUTHORITY_URL"})
    public MitIdConf mitIdConf(
            @Value("${MITID_AUTHORITY_URL}") String authorityUrl,
            @Value("${MITID_AID}") String clientId,
            @Value("${MITID_SECRET}") String secret) {
        log.info("MitID Configured...");
        return new MitIdConf(authorityUrl, clientId, secret);
    }

    @Bean
    @ConditionalOnProperty(name = {"MSID_AID", "MSID_SECRET", "MSID_TENANT"})
    public MicrosoftIdentityConf msIdentityConf(
            @Value("${MSID_AID}") String appId,
            @Value("${MSID_SECRET}") String secret,
            @Value("${MSID_TENANT}") String tenant) {
        log.info("Microsoft Identity Configured...");
        return new MicrosoftIdentityConf(appId, secret, tenant);
    }

    @Bean
    public FafnirConf fafnirConf(
            @Value("${FAFNIR_URL:http://localhost:8080}") String url,
            @Value("${FAFNIR_SUCCESS:http://localhost:8080/success}") String success,
            @Value("${FAFNIR_FAILURE:http://localhost:8080/fail}") String failure) {
        return new FafnirConf(url, success, failure);
    }

    @Bean
    @ConditionalOnBean(GoogleConf.class)
    public OAuth20Service googleOAuth (GoogleConf googleConf, FafnirConf fafnirConf) {
        return Try.of(() -> new ServiceBuilder(googleConf.getAppId())
                .apiSecret(googleConf.getSecret())
                .callback(fafnirConf.getUrl() + "/google/callback")
                .defaultScope("openid email profile")
                .build(GoogleApi20.instance())).getOrNull();
    }

    @Bean
    @ConditionalOnBean(FacebookConf.class)
    public OAuth20Service facebookOAuth (FacebookConf facebookConf, FafnirConf fafnirConf) {
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

    @Bean
    @ConditionalOnBean(AppleConf.class)
    public OAuth20Service appleOAuth(AppleConf appleConf, FafnirConf fafnirConf ) {
        return Try.of(() -> new ServiceBuilder(appleConf.getAppId())
                .apiSecret(appleConf.getSecret())
                .callback(fafnirConf.getUrl() + "/apple/callback")
                .defaultScope("openid name email")
                .responseType("code id_token")
                .build(new AppleApi())).getOrNull();
    }

    @Bean
    @ConditionalOnBean(MitIdConf.class)
    public OAuth20Service mitIdOauth(MitIdConf mitIdConf, TestConf testConf, FafnirConf fafnirConf) {
        return Try.of(() -> new ServiceBuilder(mitIdConf.getClientId())
                .apiSecret(mitIdConf.getSecret())
                .callback(fafnirConf.getUrl() + "/mitid/callback")
                .defaultScope(String.format("openid ssn %s", testConf.isEnabled() ? "mitid_demo" : "mitid"))
                .build(new MitIdApi(mitIdConf.getAuthorityUrl())))
                .getOrNull();
    }

    @Bean
    @ConditionalOnBean(MicrosoftIdentityConf.class)
    public OAuth20Service microsoftIdentityOauth (MicrosoftIdentityConf msIdentityConf, FafnirConf fafnirConf) {
        return Try.of(() -> new ServiceBuilder(msIdentityConf.getAppId())
                .apiSecret(msIdentityConf.getSecret())
                .callback(fafnirConf.getUrl() + "/msidentity/callback")
                .responseType("id_token")
                .defaultScope("openid email profile")
                .build(new MicrosoftIdentityApi(msIdentityConf.getTenant()))).getOrNull();
    }

    @Bean
    @ConditionalOnBean(DemoDataGenerator.class)
    public CommandLineRunner commandLineRunner( DemoDataGenerator demoDataGenerator) {
        return args -> {
            // User Dummy Data
            demoDataGenerator.createDemoData();
        };
    }
}
