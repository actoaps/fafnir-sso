package dk.acto.fafnir.server.util;

import dk.acto.fafnir.api.model.OrganisationData;
import dk.acto.fafnir.api.service.AdministrationService;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.Instant;


@Service
@AllArgsConstructor
@Profile("demo")
public class DemoDataGenerator {

    private AdministrationService administrationService;

    public void createDemoData(){
        var actoTest = OrganisationData.builder()
                .created(Instant.now())
                .organisationId("1")
                .organisationName("Acto")
                .provider("hazelcast")
                .build();
        var HTML24Test = OrganisationData.builder()
                .created(Instant.now())
                .organisationId("2")
                .organisationName("HTML24")
                .provider("hazelcast")
                .build();
        var jhTest = OrganisationData.builder()
                .created(Instant.now())
                .organisationId("3")
                .organisationName("Jh.dk")
                .provider("hazelcast")
                .build();
        var omTest = OrganisationData.builder()
                .created(Instant.now())
                .organisationId("4")
                .organisationName("Om.com")
                .provider("hazelcast")
                .build();
        var peTest = OrganisationData.builder()
                .created(Instant.now())
                .organisationId("5")
                .organisationName("Pe.com")
                .provider("hazelcast")
                .build();
        var kaTest = OrganisationData.builder()
                .created(Instant.now())
                .organisationId("6")
                .organisationName("Ka.eu")
                .provider("hazelcast")
                .build();
        administrationService.createOrganisation(actoTest);
        administrationService.createOrganisation(HTML24Test);
        administrationService.createOrganisation(jhTest);
        administrationService.createOrganisation(omTest);
        administrationService.createOrganisation(peTest);
        administrationService.createOrganisation(kaTest);
    }




}
