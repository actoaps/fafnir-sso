package dk.acto.fafnir.server.util.generator;

import dk.acto.fafnir.api.model.OrganisationData;
import dk.acto.fafnir.api.model.ProviderConfiguration;
import dk.acto.fafnir.api.service.AdministrationService;
import dk.acto.fafnir.api.util.DataGenerator;
import dk.acto.fafnir.server.provider.HazelcastProvider;
import dk.acto.fafnir.server.provider.SamlProvider;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;


@Component
@AllArgsConstructor
@Profile("demo")
public class DemoDataGenerator implements DataGenerator {
    private final AdministrationService administrationService;
    private final HazelcastProvider hazelcastProvider;
    private final SamlProvider samlProvider;

    @Override
    public void generateData(){
        var actoTest = OrganisationData.builder()
                .created(Instant.now())
                .organisationId("1")
                .organisationName("Acto")
                .providerConfigurations(List.of(
                        ProviderConfiguration.builder()
                                .providerId(hazelcastProvider.providerId())
                                .values(Map.of())
                                .build()
                ))
                .build();
        var HTML24Test = OrganisationData.builder()
                .created(Instant.now())
                .organisationId("2")
                .organisationName("HTML24")
                .providerConfigurations(List.of(
                        ProviderConfiguration.builder()
                                .providerId(hazelcastProvider.providerId())
                                .values(Map.of())
                                .build()
                ))
                .build();
        var jhTest = OrganisationData.builder()
                .created(Instant.now())
                .organisationId("3")
                .organisationName("Jh.dk")
                .providerConfigurations(List.of(
                        ProviderConfiguration.builder()
                                .providerId(hazelcastProvider.providerId())
                                .values(Map.of())
                                .build()
                ))
                .build();
        var omTest = OrganisationData.builder()
                .created(Instant.now())
                .organisationId("4")
                .organisationName("Om.com")
                .providerConfigurations(List.of(
                        ProviderConfiguration.builder()
                                .providerId(hazelcastProvider.providerId())
                                .values(Map.of())
                                .build()
                ))
                .build();
        var peTest = OrganisationData.builder()
                .created(Instant.now())
                .organisationId("5")
                .organisationName("Pe.com")
                .providerConfigurations(List.of(
                        ProviderConfiguration.builder()
                                .providerId(hazelcastProvider.providerId())
                                .values(Map.of())
                                .build()
                ))
                .build();
        var kaTest = OrganisationData.builder()
                .created(Instant.now())
                .organisationId("6")
                .organisationName("Ka.eu")
                .providerConfigurations(List.of(
                        ProviderConfiguration.builder()
                                .providerId(hazelcastProvider.providerId())
                                .values(Map.of())
                                .build()
                ))
                .build();
        var samlTest = OrganisationData.builder()
                .created(Instant.now())
                .organisationId("7")
                .organisationName("SAML Org")
                .providerConfigurations(List.of(
                        ProviderConfiguration.builder()
                                .providerId(samlProvider.providerId())
                                .values(Map.of(
                                        "Metadata Location", "http://localhost:8081/simplesaml/saml2/idp/metadata.php",
                                        "Registration Id", "spring-saml"
                                ))
                                .build()
                ))
                .build();


        administrationService.createOrganisation(actoTest);
        administrationService.createOrganisation(HTML24Test);
        administrationService.createOrganisation(jhTest);
        administrationService.createOrganisation(omTest);
        administrationService.createOrganisation(peTest);
        administrationService.createOrganisation(kaTest);
        administrationService.createOrganisation(samlTest);
    }




}
