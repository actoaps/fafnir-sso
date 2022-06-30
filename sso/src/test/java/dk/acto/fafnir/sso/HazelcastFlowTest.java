package dk.acto.fafnir.sso;

import dk.acto.fafnir.api.crypto.RsaKeyManager;
import dk.acto.fafnir.api.model.ClaimData;
import dk.acto.fafnir.api.model.OrganisationData;
import dk.acto.fafnir.api.model.OrganisationSubjectPair;
import dk.acto.fafnir.api.model.UserData;
import dk.acto.fafnir.api.model.conf.HazelcastConf;
import dk.acto.fafnir.api.service.AdministrationService;
import dk.acto.fafnir.client.JwtValidator;
import dk.acto.fafnir.api.model.conf.FafnirConf;
import dk.acto.fafnir.sso.provider.HazelcastProvider;
import dk.acto.fafnir.sso.provider.credentials.UsernamePasswordCredentials;
import io.vavr.control.Try;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = TestConfig.class)
class HazelcastFlowTest {
    private static final Pattern JWT_MATCHER = Pattern.compile("/success#(.+)$");

    @Autowired
    private AdministrationService administrationService;

    @Autowired
    private RsaKeyManager rsaKeyManager;

    @Autowired
    private HazelcastProvider hazelcastProvider;

    @Autowired
    private HazelcastConf hazelcastConf;

    @Autowired
    private FafnirConf fafnirConf;

    @Autowired
    private JwtValidator jwtValidator;

    @BeforeEach
    void setUp() {
        Try.run(() -> administrationService.createOrganisation(OrganisationData.DEFAULT));
        Try.run(() -> administrationService.createUser(UserData.builder()
                .created(Instant.MIN)
                .subject("om@acto.dk")
                .password("omom")
                .metaId("meta")
                .name("Oscar Mike")
                .locale(Locale.forLanguageTag("da-DK"))
                .build()
                .secure(hazelcastConf.isPasswordIsEncrypted() ? rsaKeyManager.getPublicKey() : null)));
        Try.run(() -> administrationService.createClaim(
                OrganisationSubjectPair.builder()
                        .organisationId("default")
                        .subject("om@acto.dk")
                        .build(),
                ClaimData.builder()
                .claims(List.of("User", "Admin", "Site God").toArray(String[]::new))
                .build()));
    }

    @Test
    void testFullSuccessFlow() {
        var result = hazelcastProvider.callback(UsernamePasswordCredentials.builder()
                .username("om@acto.dk")
                .password("omom")
                .organisation("default")
                .build());
        var url = result.getUrl(fafnirConf);
        assertThat(url).contains("/success#");
        var matcher = JWT_MATCHER.matcher(url);
        assertThat(matcher.find()).isTrue();
        var jwt = matcher.group(1);
        var auth = jwtValidator.decodeToken(jwt);
        assertThat(auth.getUsername()).isEqualTo("om@acto.dk");
        assertThat(auth.getName()).isEqualTo("Oscar Mike");
        assertThat(auth.getMetaId()).isEqualTo("meta");
        assertThat(auth.hasMetaId()).isTrue();
        assertThat(auth.getPassword()).isNull();
        assertThat(auth.getDetails().getLocale()).isEqualTo(Locale.forLanguageTag("da-DK"));
        assertThat(auth.getDetails().getOrganisationId()).isEqualTo("default");
        assertThat(auth.getDetails().getOrganisationName()).isEqualTo("Default Organisation");
        assertThat(auth.getDetails().getRoles()).contains("User", "Admin", "Site God");
        assertThat(auth.getDetails().getCreated()).isNotNull();
        assertThat(auth.getDetails().getCreated()).isNotEqualTo(Instant.MIN);
    }

    @Test
    void testWrongPasswordFailsFlow() {
        var result = hazelcastProvider.callback(UsernamePasswordCredentials.builder()
                .username("om@acto.dk")
                .password("momo")
                .organisation(OrganisationData.DEFAULT.getOrganisationId())
                .build());
        var url = result.getUrl(fafnirConf);
        assertThat(url).contains("/fail#");
    }
}
