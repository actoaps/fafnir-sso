package dk.acto.fafnir.sso.util.generator;

import dk.acto.fafnir.api.model.*;
import dk.acto.fafnir.api.provider.metadata.MetadataProvider;
import dk.acto.fafnir.api.service.AdministrationService;
import dk.acto.fafnir.sso.provider.HazelcastProvider;
import dk.acto.fafnir.sso.provider.SamlProvider;
import io.vavr.collection.List;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;

@Component
@AllArgsConstructor
@Profile("demo")
public class DemoDataGenerator {
    private final AdministrationService administrationService;
    private final HazelcastProvider hazelcastProvider;
    private final SamlProvider samlProvider;

    public void generateData() {
        var actoTest = OrganisationData.builder()
                .created(Instant.now())
                .organisationId("1")
                .organisationName("Acto")
                .providerConfiguration(ProviderConfiguration.builder()
                        .providerId(MetadataProvider.HAZELCAST.getProviderId())
                        .values(Map.of())
                        .build()
                )
                .build();
        var HTML24Test = OrganisationData.builder()
                .created(Instant.now())
                .organisationId("2")
                .organisationName("HTML24")
                .providerConfiguration(ProviderConfiguration.builder()
                        .providerId(MetadataProvider.HAZELCAST.getProviderId())
                        .values(Map.of())
                        .build()
                )
                .build();
        var jhTest = OrganisationData.builder()
                .created(Instant.now())
                .organisationId("3")
                .organisationName("Jh.dk")
                .providerConfiguration(ProviderConfiguration.builder()
                        .providerId(MetadataProvider.HAZELCAST.getProviderId())
                        .values(Map.of())
                        .build()
                )
                .build();
        var omTest = OrganisationData.builder()
                .created(Instant.now())
                .organisationId("4")
                .organisationName("Om.com")
                .providerConfiguration(ProviderConfiguration.builder()
                        .providerId(MetadataProvider.HAZELCAST.getProviderId())
                        .values(Map.of())
                        .build()
                )
                .build();
        var peTest = OrganisationData.builder()
                .created(Instant.now())
                .organisationId("5")
                .organisationName("Pe.com")
                .providerConfiguration(ProviderConfiguration.builder()
                        .providerId(MetadataProvider.HAZELCAST.getProviderId())
                        .values(Map.of())
                        .build()
                )
                .build();
        var kaTest = OrganisationData.builder()
                .created(Instant.now())
                .organisationId("6")
                .organisationName("Ka.eu")
                .providerConfiguration(ProviderConfiguration.builder()
                        .providerId(MetadataProvider.HAZELCAST.getProviderId())
                        .values(Map.of())
                        .build()
                )
                .build();
        var samlTest = OrganisationData.builder()
                .created(Instant.now())
                .organisationId("7")
                .organisationName("SAML Org")
                .providerConfiguration(ProviderConfiguration.builder()
                        .providerId(MetadataProvider.SAML.getProviderId())
                        .values(Map.of(
                                "Metadata Location", "http://localhost:8081/simplesaml/saml2/idp/metadata.php",
                                "Registration Id", "acto-saml"
                        ))
                        .build()
                )
                .build();

        administrationService.createOrganisation(actoTest);
        administrationService.createOrganisation(HTML24Test);
        administrationService.createOrganisation(jhTest);
        administrationService.createOrganisation(omTest);
        administrationService.createOrganisation(peTest);
        administrationService.createOrganisation(kaTest);
        administrationService.createOrganisation(samlTest);

        var testUser = UserData.builder()
                .created(Instant.now())
                .subject("admin@acto.dk")
                .name("admin")
                .password("pass")
                .locale(Locale.ENGLISH)
                .metaId("1")
                .build();

        var orgPair = OrganisationSubjectPair.builder()
                .organisationId("1")
                .subject("admin@acto.dk")
                .build();

        var claim = ClaimData.builder()
                .claims(new String[]{"admin"})
                .build();

        administrationService.createUser(testUser);
        administrationService.createClaim(orgPair, claim);
    }
}
