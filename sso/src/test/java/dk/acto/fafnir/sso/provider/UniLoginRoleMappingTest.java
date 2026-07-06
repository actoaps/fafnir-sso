package dk.acto.fafnir.sso.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import dk.acto.fafnir.sso.provider.unilogin.Institution;
import dk.acto.fafnir.sso.provider.unilogin.UserInfoResponse;
import dk.acto.fafnir.sso.provider.unilogin.UserRole;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class UniLoginRoleMappingTest {

    @Test
    void ensureEmployeeLoginRole_addsFallbackWhenOnlyLaererUnknown() {
        Set<UserRole> roles = Set.of(
            UserRole.builder().name("LÆRER").type("UNKNOWN").build()
        );

        Set<UserRole> result = UniLoginProvider.ensureEmployeeLoginRole(roles, "Medarbejder");

        assertThat(roleStrings(result)).contains("Medarbejder@EMPLOYEE");
    }

    @Test
    void ensureEmployeeLoginRole_addsFallbackWhenRolesEmptyAndAktorgruppeMedarbejder() {
        Set<UserRole> result = UniLoginProvider.ensureEmployeeLoginRole(Set.of(), "Medarbejder");

        assertThat(roleStrings(result)).containsExactly("Medarbejder@EMPLOYEE");
    }

    @Test
    void ensureEmployeeLoginRole_keepsValidEmployeeRoleWithoutFallback() {
        Set<UserRole> roles = Set.of(
            UserRole.builder().name("Lærer").type("EMPLOYEE").build()
        );

        Set<UserRole> result = UniLoginProvider.ensureEmployeeLoginRole(roles, "Medarbejder");

        assertThat(roleStrings(result)).containsExactly("Lærer@EMPLOYEE");
    }

    @Test
    void ensureEmployeeLoginRole_acceptsMedarbejderUnknownAsCompatible() {
        Set<UserRole> roles = Set.of(
            UserRole.builder().name("Medarbejder").type("UNKNOWN").build()
        );

        Set<UserRole> result = UniLoginProvider.ensureEmployeeLoginRole(roles, "Medarbejder");

        assertThat(roleStrings(result)).containsExactly("Medarbejder@UNKNOWN");
    }

    @Test
    void deduplicateInstitutions_mergesSameInstnr() {
        List<Institution> institutions = List.of(
            new Institution("12345", "Gladsaxe Skole", List.of("LÆRER@UNKNOWN")),
            new Institution("12345", "Gladsaxe Skole", List.of("Medarbejder"))
        );

        List<Institution> result = UniLoginProvider.deduplicateInstitutions(institutions);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id).isEqualTo("12345");
        assertThat(result.get(0).roles).containsExactlyInAnyOrder("LÆRER@UNKNOWN", "Medarbejder");
    }

    @Test
    void deduplicateInstitutions_keepsDistinctInstnr() {
        List<Institution> institutions = List.of(
            new Institution("11111", "Skole A", List.of()),
            new Institution("22222", "Skole B", List.of())
        );

        List<Institution> result = UniLoginProvider.deduplicateInstitutions(institutions);

        assertThat(result).hasSize(2);
    }

    @Test
    void userInfoResponse_parsesInstbrugereAlias() throws Exception {
        String json = """
            {"instbrugere":[{"instnr":"12345","instnavn":"Test Skole","ansat_roller":["Lærer"]}]}
            """;

        UserInfoResponse response = new ObjectMapper().readValue(json, UserInfoResponse.class);

        assertThat(response.getInstBrugere()).hasSize(1);
        assertThat(response.getInstBrugere().get(0).getInTnr()).isEqualTo("12345");
        assertThat(response.getInstBrugere().get(0).getAnsatRoller()).containsExactly("Lærer");
    }

    @Test
    void isEmployeeAktorgruppe_recognizesEmployeeValues() {
        assertThat(UniLoginProvider.isEmployeeAktorgruppe("Medarbejder")).isTrue();
        assertThat(UniLoginProvider.isEmployeeAktorgruppe("EMPLOYEE")).isTrue();
        assertThat(UniLoginProvider.isEmployeeAktorgruppe("ANSAT")).isTrue();
        assertThat(UniLoginProvider.isEmployeeAktorgruppe("Elev")).isFalse();
        assertThat(UniLoginProvider.isEmployeeAktorgruppe(null)).isFalse();
    }

    @Test
    void fixA_emptyRolesWithMedarbejderAktorgruppe_yieldsCompatibleEmployeeRole() {
        Set<UserRole> roles = UniLoginProvider.ensureEmployeeLoginRole(Set.of(), "Medarbejder");
        assertThat(UniLoginProvider.hasFind2LearnCompatibleEmployeeRole(roles)).isTrue();
        assertThat(roleStrings(roles)).contains("Medarbejder@EMPLOYEE");
    }

    private static List<String> roleStrings(Set<UserRole> roles) {
        return roles.stream().map(UserRole::toString).sorted().collect(Collectors.toList());
    }
}
