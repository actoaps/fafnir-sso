package dk.acto.fafnir.sso.conf;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.scribejava.apis.FacebookApi;
import com.github.scribejava.apis.GoogleApi20;
import com.github.scribejava.apis.LinkedInApi20;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.oauth.OAuth20Service;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.HazelcastInstance;
import dk.acto.fafnir.api.crypto.RsaKeyManager;
import dk.acto.fafnir.api.exception.*;
import dk.acto.fafnir.api.model.conf.FafnirConf;
import dk.acto.fafnir.api.model.conf.HazelcastConf;
import dk.acto.fafnir.api.service.AdministrationService;
import dk.acto.fafnir.api.service.hazelcast.HazelcastAdministrationService;
import dk.acto.fafnir.client.providers.PublicKeyProvider;
import dk.acto.fafnir.client.providers.builtin.RsaKeyMangerPublicKeyProvider;
import dk.acto.fafnir.sso.provider.*;
import dk.acto.fafnir.sso.service.AppleApi;
import dk.acto.fafnir.sso.service.MicrosoftIdentityApi;
import dk.acto.fafnir.sso.service.MitIdApi;
import dk.acto.fafnir.sso.util.TokenFactory;
import dk.acto.fafnir.sso.util.generator.DemoDataGenerator;
import dk.acto.fafnir.sso.model.conf.*;
import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.Optional;

@Slf4j
@Configuration
public class BeanConf {

    @Bean
    @ConditionalOnProperty(name = {"ECONOMIC_AST", "ECONOMIC_AGT"})
    public EconomicCustomerProvider economicCustomerProvider(
            @Value("${ECONOMIC_AST}") final String secret,
            @Value("${ECONOMIC_AGT}") final String grant,
            final TokenFactory tokenFactory,
            final AdministrationService administrationService) {
        log.info("Initialising Economic Customer Configuration...");
        var conf = new EconomicConf(secret, grant);
        return new EconomicCustomerProvider(tokenFactory, conf, administrationService);
    }

    @Bean
    @ConditionalOnProperty(name = {"FACEBOOK_AID", "FACEBOOK_SECRET"})
    public FacebookProvider facebookProvider(
            @Value("${FACEBOOK_AID}") final String appId,
            @Value("${FACEBOOK_SECRET}") final String secret,
            final FafnirConf fafnirConf,
            final TokenFactory tokenFactory,
            final ObjectMapper objectMapper,
            final AdministrationService administrationService) {
        log.info("Initialising Facebook Configuration...");
        return Try.of(() -> new ServiceBuilder(appId)
                .apiSecret(secret)
                .callback(fafnirConf.getUrl() + "/facebook/callback")
                .defaultScope("email")
                .build(FacebookApi.instance()))
                .map(oAuth20Service -> new FacebookProvider(
                        tokenFactory, objectMapper, oAuth20Service, administrationService
                ))
                .toJavaOptional()
                .orElseThrow(FacebookConfigurationBroken::new);
    }

    @Bean
    @ConditionalOnProperty(name = {"GOOGLE_AID", "GOOGLE_SECRET"})
    public GoogleProvider googleProvider(
            @Value("${GOOGLE_AID}") final String appId,
            @Value("${GOOGLE_SECRET}") final String secret,
            final FafnirConf fafnirConf,
            final TokenFactory tokenFactory,
            final AdministrationService administrationService) {
        log.info("Initialising Google Configuration...");
        return Try.of(() -> new ServiceBuilder(appId)
                .apiSecret(secret)
                .callback(fafnirConf.getUrl() + "/google/callback")
                .defaultScope("openid email profile")
                .build(GoogleApi20.instance()))
                .map(oAuth20Service -> new GoogleProvider(
                        oAuth20Service, tokenFactory, administrationService))
                .toJavaOptional()
                .orElseThrow(GoogleConfigurationBroken::new);
    }

    @Bean
    @ConditionalOnProperty(name = {"LINKED_IN_AID", "LINKED_IN_SECRET"})
    public LinkedInProvider linkedInProvider(
            @Value("${LINKED_IN_AID}") final String appId,
            @Value("${LINKED_IN_SECRET}") final String secret,
            final FafnirConf fafnirConf,
            final TokenFactory tokenFactory,
            final ObjectMapper objectMapper,
            final AdministrationService administrationService) {
        log.info("Initialising LinkedIn Configuration...");
        return Try.of(() -> new ServiceBuilder(appId)
                .apiSecret(secret)
                .callback(fafnirConf.getUrl() + "/linkedin/callback")
                .defaultScope("r_liteprofile r_emailaddress") //r_fullprofile
                .build(LinkedInApi20.instance()))
                .map(oAuth20Service -> new LinkedInProvider(
                        oAuth20Service, tokenFactory, objectMapper, administrationService ))
                .toJavaOptional()
                .orElseThrow(LinkedInConfigurationBroken::new);
    }

    @Bean
    @ConditionalOnProperty(name = {"UL_AID", "UL_SECRET", "UL_WS_USER", "UL_WS_PASS"})
    public UniLoginProvider uniLoginProvider(
            @Value("${UL_AID}") final String appId,
            @Value("${UL_SECRET}") final String secret,
            @Value("${UL_WS_USER}") final String wsUser,
            @Value("${UL_WS_PASS}") final String wsPass,
            @Value("${UL_SSO:false}") final boolean sso,
            final FafnirConf fafnirConf,
            final TokenFactory tokenFactory) {
        log.info("Initialising UniLogin Configuration...");
        var ulconf = new UniLoginConf(appId, secret, wsUser, wsPass, sso);
        var helper = new UniLoginHelper(ulconf, fafnirConf);
        return new UniLoginProvider(fafnirConf, helper, tokenFactory);

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
    public PublicKeyProvider publicKeyProvider(RsaKeyManager rsaKeyManager) {
        return new RsaKeyMangerPublicKeyProvider(rsaKeyManager);
    }

    @Bean
    public AdministrationService administrationService(HazelcastInstance hazelcastInstance, HazelcastConf hazelcastConf, PublicKeyProvider publicKeyProvider) {
        return new HazelcastAdministrationService(hazelcastInstance, hazelcastConf, publicKeyProvider);
    }

    @Bean
    @ConditionalOnProperty(name = "TEST_ENABLED")
    public TestProvider testProvider(TokenFactory tokenFactory, FafnirConf fafnirConf) {
        return new TestProvider(tokenFactory, fafnirConf);
    }

    @Bean
    @ConditionalOnProperty(name = {"APPLE_AID", "APPLE_SECRET"})
    public AppleProvider appleProvider(
            @Value("${APPLE_AID}") final String appId,
            @Value("${APPLE_SECRET}") final String secret,
            final FafnirConf fafnirConf,
            final TokenFactory tokenFactory
            ) {
        log.info("Initialising Apple Configuration...");
        return Try.of(() -> new ServiceBuilder(appId)
                .apiSecret(secret)
                .callback(fafnirConf.getUrl() + "/apple/callback")
                .defaultScope("openid name email")
                .responseType("code id_token")
                .build(new AppleApi()))
                .map(oAuth20Service -> new AppleProvider(oAuth20Service, tokenFactory))
                .toJavaOptional()
                .orElseThrow(AppleConfigurationBroken::new);
    }

    @Bean
    @ConditionalOnProperty(name = {"MITID_AID", "MITID_SECRET", "MITID_AUTHORITY_URL"})
    public MitIdProvider mitIdProvider(
            @Value("${MITID_AUTHORITY_URL}") final String authorityUrl,
            @Value("${MITID_AID}") final String clientId,
            @Value("${MITID_SECRET}") final String secret,
            @Value("TEST_ENABLED") final Optional<String> test,
            final FafnirConf fafnirConf,
            final TokenFactory tokenFactory,
            final ObjectMapper objectMapper,
            final AdministrationService administrationService) {
        log.info("Initialising MitID Configuration...");
        return Try.of(() -> new ServiceBuilder(clientId)
                        .apiSecret(secret)
                        .callback(fafnirConf.getUrl() + "/mitid/callback")
                        .defaultScope(String.format("openid ssn %s", test.isPresent() ? "mitid_demo" : "mitid"))
                        .build(new MitIdApi(authorityUrl)))
                .map(oAuth20Service -> new MitIdProvider(
                        oAuth20Service,
                        tokenFactory,
                        objectMapper,
                        administrationService,
                        authorityUrl,
                        test.isPresent()))
                .toJavaOptional()
                .orElseThrow(MitIdConfigurationBroken::new);
    }

    @Bean
    @ConditionalOnProperty(name = {"MSID_AID", "MSID_SECRET", "MSID_TENANT"})
    public MicrosoftIdentityProvider msIdentityProvider(
            @Value("${MSID_AID}") final String appId,
            @Value("${MSID_SECRET}") final String secret,
            @Value("${MSID_TENANT}") final String tenant,
            final FafnirConf fafnirConf,
            final TokenFactory tokenFactory,
            final AdministrationService administrationService) {
        log.info("Initialising Microsoft Identity Configuration...");
        return Try.of(() -> new ServiceBuilder(appId)
                .apiSecret(secret)
                .callback(fafnirConf.getUrl() + "/msidentity/callback")
                .responseType("id_token")
                .defaultScope("openid email profile")
                .build(new MicrosoftIdentityApi(tenant)))
                .map(oAuth20Service -> new MicrosoftIdentityProvider(
                        tokenFactory, oAuth20Service, administrationService))
                .toJavaOptional()
                .orElseThrow(MicrosoftConfigurationBroken::new);
    }

    @Bean
    public FafnirConf fafnirConf(
            @Value("${FAFNIR_URL:http://localhost:8080}") String url,
            @Value("${FAFNIR_SUCCESS:http://localhost:8080/success}") String success,
            @Value("${FAFNIR_FAILURE:http://localhost:8080/fail}") String failure) {
        return new FafnirConf(url, success, failure);
    }


    @Bean
    @ConditionalOnBean(DemoDataGenerator.class)
    public CommandLineRunner commandLineRunner(DemoDataGenerator demoDataGenerator) {
        return args -> demoDataGenerator.generateData();
    }
}
