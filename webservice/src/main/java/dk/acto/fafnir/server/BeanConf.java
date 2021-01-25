package dk.acto.fafnir.server;

import com.github.scribejava.apis.FacebookApi;
import com.github.scribejava.apis.GoogleApi20;
import com.github.scribejava.apis.LinkedInApi20;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.oauth.OAuth20Service;
import com.hazelcast.client.config.ClientConfig;
import dk.acto.fafnir.api.model.conf.HazelcastConf;
import dk.acto.fafnir.server.model.conf.*;
import dk.acto.fafnir.server.services.AppleApi;
import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;

@Slf4j
@Configuration
public class BeanConf {

    @Bean
    @Lazy
    public EconomicConf economicConf(
            @Value("${ECONOMIC_AST}") String secret,
            @Value("${ECONOMIC_AGT}") String grant) {
        log.info("Economic Customer Configured...");
        return new EconomicConf(secret, grant);
    }

    @Bean
    @Lazy
    public FacebookConf facebookConf(
            @Value("${FACEBOOK_AID}") String appId,
            @Value("${FACEBOOK_SECRET}") String secret) {
        log.info("Facebook Configured...");
        return new FacebookConf(appId, secret);
    }

    @Bean
    @Lazy
    public GoogleConf googleConf(
            @Value("${GOOGLE_AID}") String appId,
            @Value("${GOOGLE_SECRET}") String secret) {
        log.info("Google Configured...");
        return new GoogleConf(appId, secret);
    }

    @Bean
    @Lazy
    public LinkedInConf linkedInConf(
            @Value("${LINKED_IN_AID}") String appId,
            @Value("${LINKED_IN_SECRET}") String secret) {
        log.info("LinkedIn Configured...");
        return new LinkedInConf(appId, secret);
    }

    @Bean
    @Lazy
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
    @Lazy
    public TestConf testConf() {
        log.info("Test Configured...");
        return new TestConf(true);
    }

    @Bean
    @Lazy
    public AppleConf appleConf(
            @Value("${APPLE_AID}") String appId,
            @Value("${APPLE_SECRET}") String secret) {
        log.info("Apple Configured...");
        return new AppleConf(appId, secret);
    }

    @Bean
    public FafnirConf fafnirConf(
            @Value("${FAFNIR_URL:http://localhost:8080}") String url,
            @Value("${FAFNIR_SUCCESS:http://localhost:8080/success}") String success,
            @Value("${FAFNIR_FAILURE:http://localhost:8080/fail}") String failure) {
        return new FafnirConf(url, success, failure);
    }

    @Bean
    @Lazy
    public OAuth20Service googleOAuth (GoogleConf googleConf, FafnirConf fafnirConf) {
        return Try.of(() -> new ServiceBuilder(googleConf.getAppId())
                .apiSecret(googleConf.getSecret())
                .callback(fafnirConf.getUrl() + "/google/callback")
                .defaultScope("openid email profile")
                .build(GoogleApi20.instance())).getOrNull();
    }

    @Bean
    @Lazy
    public OAuth20Service facebookOAuth (FacebookConf facebookConf, FafnirConf fafnirConf) {
        return Try.of(() -> new ServiceBuilder(facebookConf.getAppId())
                .apiSecret(facebookConf.getSecret())
                .callback(fafnirConf.getUrl() + "/facebook/callback")
                .defaultScope("email")
                .build(FacebookApi.instance())).getOrNull();
    }

    @Bean
    @Lazy
    public OAuth20Service linkedInOAuth(LinkedInConf linkedInConf, FafnirConf fafnirConf ) {
        return Try.of(() -> new ServiceBuilder(linkedInConf.getAppId())
                .apiSecret(linkedInConf.getSecret())
                .callback(fafnirConf.getUrl() + "/linkedin/callback")
                .defaultScope("r_liteprofile r_emailaddress") //r_fullprofile
                .build(LinkedInApi20.instance())).getOrNull();
    }

    @Bean
    @Lazy
    public OAuth20Service appleOAuth(AppleConf appleConf, FafnirConf fafnirConf ) {
        return Try.of(() -> new ServiceBuilder(appleConf.getAppId())
                .apiSecret(appleConf.getSecret())
                .callback(fafnirConf.getUrl() + "/apple/callback")
                .defaultScope("openid name email")
                .responseType("code id_token")
                .build(new AppleApi())).getOrNull();
    }
}
