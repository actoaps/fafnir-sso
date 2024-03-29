package dk.acto.fafnir.iam.conf;

import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.HazelcastInstance;
import com.samskivert.mustache.Mustache;
import dk.acto.fafnir.api.exception.*;
import dk.acto.fafnir.api.model.ClaimData;
import dk.acto.fafnir.api.model.OrganisationData;
import dk.acto.fafnir.api.model.OrganisationSubjectPair;
import dk.acto.fafnir.api.model.UserData;
import dk.acto.fafnir.api.model.conf.HazelcastConf;
import dk.acto.fafnir.api.service.AdministrationService;
import dk.acto.fafnir.api.service.CryptoService;
import dk.acto.fafnir.api.service.hazelcast.CryptoServiceImpl;
import dk.acto.fafnir.api.service.hazelcast.HazelcastAdministrationService;
import dk.acto.fafnir.client.providers.PublicKeyProvider;
import dk.acto.fafnir.client.providers.builtin.RestfulPublicKeyProvider;
import dk.acto.fafnir.iam.security.IAMRoles;
import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

@Slf4j
@Configuration
public class BeanConf {

    @Bean
    public Mustache.Compiler compiler(Mustache.TemplateLoader templateLoader) {
        return Mustache.compiler()
                .defaultValue("")
                .nullValue("")
                .withLoader(templateLoader);
    }

    @Bean
    @ConditionalOnProperty(name = "HAZELCAST_TCP_IP_ADDRESS")
    public ClientConfig hazelcastInstanceConf(@Value("${HAZELCAST_TCP_IP_ADDRESS}") String address) {
        log.info("Hazelcast TCP/IP Connection Configured...");
        var config = new ClientConfig();
        config.getNetworkConfig().addAddress(address);
        return config;
    }

    @Bean
    public PublicKeyProvider publicKeyProvider(
            @Value("${FAFNIR_URL:http://localhost}") final String fafnirUrl,
            @Value("${FAFNIR_PORT:8080}") final String fafnirPort) {
        return new RestfulPublicKeyProvider(fafnirUrl, fafnirPort);
    }

    @Bean
    @ConditionalOnProperty(name = "IAM_ADMIN_PASSWORD")
    public CommandLineRunner commandLineRunner(
            AdministrationService administrationService,
            @Value("${IAM_ADMIN_SUBJECT:ADMIN}") String subject,
            @Value("${IAM_ADMIN_PASSWORD}") String password) {
        return args -> {
            var user = Try.of(() -> administrationService.createUser(UserData.builder()
                            .name("Fafnir Admin")
                            .password(password)
                            .subject(subject)
                            .created(Instant.now())
                            .locale(Locale.US)
                            .build()))
                    .recover(UserAlreadyExists.class, administrationService.readUser(subject))
                    .toJavaOptional()
                    .orElseThrow(NoUser::new);

            var org = Try.of(() -> administrationService.createOrganisation(OrganisationData.DEFAULT))
                    .recover(OrganisationAlreadyExists.class, administrationService.readOrganisation(OrganisationData.DEFAULT.getOrganisationId()))
                    .toJavaOptional()
                    .orElseThrow(NoOrganisation::new);

            var pair = OrganisationSubjectPair.builder()
                    .organisationId(org.getOrganisationId())
                    .subject(user.getSubject())
                    .build();

            var claim = Try.of(() -> administrationService.createClaim(
                            pair,
                            ClaimData.builder()
                                    .claims(List.of(IAMRoles.FAFNIR_ADMIN.toString()).toArray(String[]::new))
                                    .build()))
                    .recover(ClaimAlreadyExists.class, administrationService.readClaims(pair))
                            .toJavaOptional()
                            .orElseThrow(NoClaimData::new);

            log.info(String.format("Successfully created %s user with following claims: %s", subject, claim));
        };
    }
}
