package dk.acto.fafnir.api.service.hazelcast;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import dk.acto.fafnir.api.exception.NoSuchClaim;
import dk.acto.fafnir.api.exception.NoSuchOrganisation;
import dk.acto.fafnir.api.exception.NoSuchUser;
import dk.acto.fafnir.api.model.*;
import dk.acto.fafnir.api.model.conf.HazelcastConf;
import dk.acto.fafnir.api.provider.metadata.MetadataProvider;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.Instant;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HazelcastAdministrationServiceTest {
    private final HazelcastAdministrationService subject = getService();

    private static HazelcastAdministrationService getService() {
        var config = new Config();
        config.setProperty("hazelcast.shutdownhook.enabled", "false");
        var network = config.getNetworkConfig();
        network.getJoin().getMulticastConfig().setEnabled(false);
        network.getJoin().getTcpIpConfig().setEnabled(true);
        var instance = Hazelcast.newHazelcastInstance(config);

        return new HazelcastAdministrationService(
                instance,
                new HazelcastConf(true,
                        false,
                        true,
                        "TEST"
                ),
                () -> null
        );
    }

    @Test
    void testFlux() {
        StepVerifier.setDefaultTimeout(Duration.ofSeconds(5));

        var userData1 = UserData.builder()
                .subject("FLUX_1")
                .password("abob")
                .name("Alpha Bravo")
                .build();
        var userData2 = UserData.builder()
                .subject("FLUX_2")
                .password("abob")
                .name("Alpha Bravo")
                .build();
        var userData3 = UserData.builder()
                .subject("FLUX_3")
                .password("abob")
                .name("Alpha Bravo")
                .build();

        StepVerifier.create(subject.getUserFlux().autoConnect())
                .then(() -> subject.createUser(userData1))
                .assertNext(x -> assertThat(x.getSubject()).isEqualTo(userData1.getSubject()))
                .then(() -> subject.createUser(userData2))
                .assertNext(x -> assertThat(x.getSubject()).isEqualTo(userData2.getSubject()))
                .then(() -> subject.createUser(userData3))
                .assertNext(x -> assertThat(x.getSubject()).isEqualTo(userData3.getSubject()))
                .then(() -> subject.updateUser(userData3.toBuilder().password("newpass").build()))
                .assertNext(x -> assertThat(x.getSubject()).isEqualTo(userData3.getSubject()))
                .thenCancel()
                .verify();

        var orgData1 = OrganisationData.builder()
                .organisationId("FLUX_1")
                .providerConfiguration(ProviderConfiguration.builder()
                        .providerId(MetadataProvider.HAZELCAST.getProviderId())
                        .build())
                .organisationName("Flux Test 1")
                .created(Instant.now())
                .build();
        StepVerifier.create(subject.getOrganisationFlux().autoConnect())
                .then(() -> subject.createOrganisation(orgData1))
                .assertNext(x -> assertThat(x.getOrganisationId()).isEqualTo(orgData1.getOrganisationId()))
                .thenCancel()
                .verify();

        StepVerifier.create(subject.getUserDeletionFlux().autoConnect())
                .then(() -> subject.deleteUser(userData1.getSubject()))
                .assertNext(x -> assertThat(x).isEqualTo(userData1.getSubject()))
                .thenCancel()
                .verify();

        StepVerifier.create(subject.getOrganisationDeletionFlux().autoConnect())
                .then(() -> subject.deleteOrganisation(orgData1.getOrganisationId()))
                .assertNext(x -> assertThat(x).isEqualTo(orgData1.getOrganisationId()))
                .thenCancel()
                .verify();
    }

    @Test
    void fluxNoUpdate() {
        StepVerifier.setDefaultTimeout(Duration.ofSeconds(5));

        var userData1 = UserData.builder()
                .subject("FLUX_UPDATE_1")
                .password("abob")
                .name("Alpha Bravo")
                .build();
        var userData2 = UserData.builder()
                .subject("FLUX_UPDATE_2")
                .password("abob")
                .name("Alpha Bravo")
                .build();

        StepVerifier.create(subject.getUserFlux(false).autoConnect())
                .then(() -> subject.createUser(userData1))
                .assertNext(x -> assertThat(x.getSubject()).isEqualTo(userData1.getSubject()))
                .then(() -> subject.updateUser(userData1.toBuilder().password("newpass").build()))
                .then(() -> subject.createUser(userData2))
                .assertNext(x -> assertThat(x.getSubject()).isEqualTo(userData2.getSubject()))
                .thenCancel()
                .verify();
    }

    @Test
    void createAndReadUser() {
        var ab = UserData.builder()
                .subject("ALPHA_BRAVO")
                .password("abob")
                .name("Alpha Bravo")
                .build();
        var result = subject.createUser(ab);
        assertThat(result).isNotNull();
        assertThat(result.getCreated()).isNotNull();
        assertThat(result.getSubject()).isNotNull();
        assertThat(result.getPassword()).isNotNull();
        assertThat(result.getName()).isNotNull();

        var readResult = subject.readUser(ab.getSubject());
        assertThat(result).isEqualTo(readResult);
    }

    @Test
    void readUsers() {
        var bc = UserData.builder()
                .subject("BRAVO_CHARLIE")
                .password("bobcoc")
                .name("Bravo Charlie")
                .build();
        var cd = UserData.builder()
                .subject("CHARLIE_DELTA")
                .password("cocdod")
                .name("Charlie Delta")
                .build();

        var result = Stream.of(
                        subject.createUser(bc),
                        subject.createUser(cd))
                .toArray(UserData[]::new);

        assertThat(subject.readUsers()).contains(result);
        assertThat(subject.readUsers(0L).getPageData()).contains(result);
    }

    @Test
    void updateUser() {
        var de = UserData.builder()
                .subject("DELTA_ECHO")
                .password("dode")
                .name("Delta Echo")
                .build();
        var temp = subject.createUser(de);
        assertThat(temp).isNotNull();
        var result = subject.updateUser(UserData.builder()
                .subject("DELTA_ECHO")
                .name("Echo Delta")
                .build());
        assertThat(result).isNotNull();
        assertThat(result.getCreated()).isEqualTo(temp.getCreated());
        assertThat(result.getSubject()).isEqualTo(temp.getSubject());
        assertThat(result.getPassword()).isEqualTo(temp.getPassword());
        assertThat(result.getName()).isNotEqualTo(temp.getName());
        var secondResult = subject.readUser(result.getSubject());
        assertThat(secondResult).isEqualTo(result);
    }

    @Test
    void deleteUser() {
        var ef = UserData.builder()
                .subject("ECHO_FOXTROT")
                .password("efof")
                .name("Echo Foxtrot")
                .build();
        var temp = subject.createUser(ef);
        assertThat(temp).isNotNull();
        var result = subject.deleteUser(ef.getSubject());
        assertThat(temp).isEqualTo(result);
        assertThat(subject.readUsers()).doesNotContain(result);
    }

    @Test
    void createAndReadOrganisation() {
        var php25 = OrganisationData.builder()
                .organisationId("PHP_25")
                .organisationName("PHP 25")
                .build();
        var result = subject.createOrganisation(php25);
        assertThat(result).isNotNull();
        assertThat(result.getCreated()).isNotNull();
        assertThat(result.getOrganisationId()).isNotNull();
        assertThat(result.getOrganisationName()).isNotNull();
        var readResult = subject.readOrganisation(php25.getOrganisationId());
        assertThat(readResult).isEqualTo(result);
    }

    @Test
    void readOrganisations() {
        var acto = OrganisationData.builder()
                .organisationId("ACTO_APS")
                .organisationName("Acto Aps")
                .build();
        var bt = OrganisationData.builder()
                .organisationId("BINARY_THERAPY")
                .organisationName("Binary Therapy Heavy Industries")
                .build();

        var result = Stream.of(subject.createOrganisation(acto),
                        subject.createOrganisation(bt))
                .toArray(OrganisationData[]::new);

        assertThat(subject.readOrganisations()).contains(result);
        assertThat(subject.readOrganisations(0L).getPageData()).contains(result);
    }

    @Test
    void updateOrganisation() {
        var af = OrganisationData.builder()
                .organisationId("ALGE_FARM")
                .organisationName("Algefarmen A/S")
                .contactEmail("info@algefarmen.as")
                .build();
        var temp = subject.createOrganisation(af);
        assertThat(temp).isNotNull();
        var result = subject.updateOrganisation(OrganisationData.builder()
                .organisationId("ALGE_FARM")
                .organisationName("Algefarmen ApS")
                .build());
        assertThat(result).isNotNull();
        assertThat(result.getCreated()).isEqualTo(temp.getCreated());
        assertThat(result.getOrganisationId()).isEqualTo(temp.getOrganisationId());
        assertThat(result.getOrganisationName()).isNotEqualTo(temp.getOrganisationName());
        assertThat(result.getContactEmail()).isEqualTo(temp.getContactEmail());
        var secondResult = subject.readOrganisation(result.getOrganisationId());
        assertThat(secondResult).isEqualTo(result);
    }

    @Test
    void deleteOrganisation() {
        var mi = OrganisationData.builder()
                .organisationId("MURDER_INC")
                .organisationName("Murder Inc.")
                .build();
        var temp = subject.createOrganisation(mi);
        assertThat(temp).isNotNull();
        var result = subject.deleteOrganisation(mi.getOrganisationId());
        assertThat(temp).isEqualTo(result);
        assertThat(subject.readOrganisations()).doesNotContain(result);
    }

    @Test
    void createAndReadClaim() {
        var fg = UserData.builder()
                .subject("FOXTROT_GOLF")
                .password("fofgog")
                .name("Foxtrot Golf")
                .build();
        var fgorg = OrganisationData.builder()
                .organisationId("FOXTROT_GOLF_INC")
                .organisationName("Foxtrot Golf Inc.")
                .build();
        var fgclaims = ClaimData.builder()
                .claims(Stream.of("Picker", "Grinner", "Lover", "Sinner")
                        .toArray(String[]::new))
                .build();
        var fgpair = OrganisationSubjectPair.builder()
                .subject(fg.getSubject())
                .organisationId(fgorg.getOrganisationId())
                .build();

        assertThatThrownBy(() -> subject.createClaim(fgpair, fgclaims))
                .isInstanceOf(NoSuchUser.class);
        subject.createUser(fg);
        assertThatThrownBy(() -> subject.createClaim(fgpair, fgclaims))
                .isInstanceOf(NoSuchOrganisation.class);
        subject.createOrganisation(fgorg);
        var temp = subject.createClaim(fgpair, fgclaims);
        assertThat(temp).isNotNull().isEqualTo(fgclaims);
        var read = subject.readClaims(fgpair);
        assertThat(read).isNotNull().isEqualTo(temp);
    }

    @Test
    void updateClaims() {
        var gh = UserData.builder()
                .subject("GOLF_HOTEL")
                .password("goghoh")
                .name("Golf Hotel")
                .build();
        var ghorg = OrganisationData.builder()
                .organisationId("GOLF_HOTEL_INC")
                .organisationName("Golf Hotel Inc.")
                .build();
        var ghclaims = ClaimData.builder()
                .claims(Stream.of("Picker", "Grinner", "Lover", "Sinner")
                        .toArray(String[]::new))
                .build();
        var ghpair = OrganisationSubjectPair.builder()
                .organisationId(ghorg.getOrganisationId())
                .subject(gh.getSubject())
                .build();
        var uclaims = ClaimData.builder()
                .claims(Stream.of("Joker", "Smoker", "Midnight Toker")
                        .toArray(String[]::new))
                .build();
        subject.createUser(gh);
        subject.createOrganisation(ghorg);
        subject.createClaim(ghpair, ghclaims);
        var result = subject.updateClaims(ghpair, uclaims);
        assertThat(ghclaims).isNotEqualTo(result);
        assertThat(result.getClaims()).doesNotContain(ghclaims.getClaims());
    }

    @Test
    void deleteClaims() {
        var hi = UserData.builder()
                .subject("HOTEL_INDIA")
                .password("hohi")
                .name("Hello Internet")
                .build();
        var hiorg = OrganisationData.builder()
                .organisationId("HOTEL_INDIA_INC")
                .organisationName("Hello Internet Inc.")
                .build();
        var hiclaims = ClaimData.builder()
                .claims(Stream.of("Picker", "Grinner", "Lover", "Sinner")
                        .toArray(String[]::new))
                .build();
        var hipair = OrganisationSubjectPair.builder()
                .organisationId(hiorg.getOrganisationId())
                .subject(hi.getSubject())
                .build();
        subject.createUser(hi);
        subject.createOrganisation(hiorg);
        subject.createClaim(hipair, hiclaims);
        var result = subject.deleteClaims(hipair);
        assertThat(result).isEqualTo(hiclaims);
        assertThatThrownBy(() -> subject.deleteClaims(hipair))
                .isInstanceOf(NoSuchClaim.class);
    }
}
