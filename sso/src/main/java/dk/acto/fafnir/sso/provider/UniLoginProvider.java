package dk.acto.fafnir.sso.provider;

import dk.acto.fafnir.api.model.*;
import dk.acto.fafnir.api.model.conf.FafnirConf;
import dk.acto.fafnir.sso.provider.unilogin.Institution;
import dk.acto.fafnir.sso.provider.unilogin.UserRole;
import dk.acto.fafnir.sso.util.TokenFactory;
import https.unilogin.Institutionstilknytning;
import https.wsibruger_unilogin_dk.ws.WsiBruger;
import https.wsiinst_unilogin_dk.ws.WsiInst;
import io.vavr.control.Try;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;


@Slf4j
@AllArgsConstructor
public class UniLoginProvider {
    private final FafnirConf fafnirConf;
    private final UniLoginHelper uniloginHelper;
    private final TokenFactory tokenFactory;

    public String authenticate() {
        return uniloginHelper.getAuthorizationUrl();
    }

    public String callback(String user, String timestamp, String auth) {
        var validAccess = uniloginHelper.isValidAccess(user, timestamp, auth);
        if (validAccess) {
            var institutionList = Try.of(() -> this.getInstitutionList(user)).getOrElse(Collections.emptyList());
            if (institutionList.size() > 1 ) {
                return uniloginHelper.getChooseInstitutionUrl(user, timestamp, auth);
            } else if (institutionList.size() == 1) {
                return callbackWithInstitution(user, timestamp, auth, institutionList.get(0).getId());
            } else {
                log.error("User does not belong to an institution failure for UniLoginProvider");
                return this.getFailureUrl(FailureReason.CONNECTION_FAILED);
            }
        } else {
            log.error("Authentication failed for UniLoginProvider");
            return this.getFailureUrl(FailureReason.AUTHENTICATION_FAILED);
        }
    }

    private Optional<Institution> getInstitutionFromId(String institutionId) {
        var wsiInst = new WsiInst();
        var wsiInstPortType = wsiInst.getWsiInstPort();
        try {
            var inst = wsiInstPortType.hentInstitution(uniloginHelper.getWsUsername(), uniloginHelper.getWsPassword(), institutionId);
            return Optional.of(Institution.builder()
                    .id(inst.getInstnr())
                    .name(inst.getInstnavn())
                    .build());
        } catch (https.wsiinst_unilogin_dk.ws.AuthentificationFault authentificationFault) {
            log.error(authentificationFault.getMessage(), authentificationFault);
            return Optional.empty();
        }
    }

    private Set<UserRole> getUserRoles(String institutionId, String userId) {
        var wsiBruger = new WsiBruger();
        var wsiBrugerPortType = wsiBruger.getWsiBrugerPort();
        try {
            var institutionstilknytninger = wsiBrugerPortType.hentBrugersInstitutionstilknytninger(uniloginHelper.getWsUsername(), uniloginHelper.getWsPassword(), userId);
            institutionstilknytninger = institutionstilknytninger.stream()
                    .filter(til -> institutionId.equals(til.getInstnr()))
                    .collect(Collectors.toList());
            return toUserRoles(institutionstilknytninger);
        } catch (https.wsibruger_unilogin_dk.ws.AuthentificationFault authentificationFault) {
            log.error(authentificationFault.getMessage(), authentificationFault);
            return Collections.emptySet();
        }
    }

    /**
     * There is three role types:
     * <ul>
     * <li>EMPLOYEE</li>
     * <li>EMP_EXTERNAL</li>
     * <li>PUPIL</li>
     * </ul>
     * <p>
     * AnsatRolle: "Lærer", "Pædagog", "Vikar", "Leder", "Ledelse", "TAP", "Konsulent"
     * EksternRolle: "Ekstern", "Praktikant"
     * ElevRolle: "Barn", "Elev", "Studerende"
     *
     * @param institutionstilknytninger
     * @return roles
     */
    private Set<UserRole> toUserRoles(java.util.List<https.unilogin.Institutionstilknytning> institutionstilknytninger) {
        Set<UserRole> roles = new HashSet<>();
        for (var institutionstilknytning : institutionstilknytninger) {
            var ansat = institutionstilknytning.getAnsat();
            var ekstern = institutionstilknytning.getEkstern();
            var elev = institutionstilknytning.getElev();
            if (ansat != null) {
                ansat.getRolle()
                        .forEach(ansatrolle -> UserRole.builder().name(ansatrolle.name()).type("EMPLOYEE").build());
            }
            if (ekstern != null) {
                roles.add(UserRole.builder()
                        .name(ekstern.getRolle().name())
                        .type("EMP_EXTERNAL")
                        .build());
            }
            if (elev != null) {
                roles.add(UserRole.builder()
                        .name(elev.getRolle().name())
                        .type("PUPIL")
                        .build());
            }
        }
        return roles;
    }

    /**
     * In this moment the UniLogin does NOT contain any name, like first name or last name.
     * See https://viden.stil.dk/pages/viewpage.action?pageId=5638128
     * It's only the UniLogin package named myndighedspakken, that can deliver sensitive data
     *
     * @param userId, from UniLogin
     * @return full name for that userId
     */
    private String getUserFullNameFromId(String userId) {
        return userId;
    }

    public String callbackWithInstitution(String userId, String timestamp, String auth, String institutionId) {
        var name = getUserFullNameFromId(userId); //jwt:name, full name of user
        final var orgName = getInstitutionFromId(institutionId)
                .map(Institution::getName)
                .orElseThrow(() -> new RuntimeException("No institution")); // jwt:org_name, the organisation name of the user

        var validAccess = uniloginHelper.isValidAccess(userId, timestamp, auth);
        if (validAccess) {
            var roles = this.getUserRoles(institutionId, userId);

            var subjectActual = UserData.builder()
                    .subject(userId)
                    .name(name)
                    .build();
            var orgActual = OrganisationData.builder()
                    .organisationId(institutionId)
                    .organisationName(orgName)
                    .build();
            var claimsActual = ClaimData.builder()
                    .claims(roles.stream()
                            .map(UserRole::toString)
                            .toArray(String[]::new))
                    .build();

            var jwt = tokenFactory.generateToken(subjectActual, orgActual, claimsActual, ProviderMetaData.builder()
                            .providerName("UniLogin")
                            .providerId("unilogin")
                            .organisationSupport(OrganisationSupport.NATIVE)
                            .inputs(List.of())
                    .build());
            return fafnirConf.getSuccessRedirect() + "#" + jwt;
        } else {
            log.error("Authentication failed for UniLoginProvider");
            return this.getFailureUrl(FailureReason.AUTHENTICATION_FAILED);
        }
    }

    public List<Institution> getInstitutionList(String userId) {
        var wsiBruger = new WsiBruger();
        var wsiBrugerPortType = wsiBruger.getWsiBrugerPort();
        List<Institutionstilknytning> institutionstilknytninger;

        var wsiInst = new WsiInst();
        var wsiInstPortType = wsiInst.getWsiInstPort();
        try {
            institutionstilknytninger = wsiBrugerPortType.hentBrugersInstitutionstilknytninger(uniloginHelper.getWsUsername(), uniloginHelper.getWsPassword(), userId);
            return institutionstilknytninger.stream().map((institutionstilknytning -> {
                var instName = "";
                try {
                    var inst = wsiInstPortType.hentInstitution(uniloginHelper.getWsUsername(), uniloginHelper.getWsPassword(), institutionstilknytning.getInstnr());
                    instName = inst.getInstnavn();
                } catch (https.wsiinst_unilogin_dk.ws.AuthentificationFault authentificationFault) {
                    log.error(authentificationFault.getMessage(), authentificationFault);
                }
                var roleNames = toUserRoles(institutionstilknytninger).stream()
                        .map(UserRole::toString)
                        .collect(Collectors.toList());
                return Institution.builder()
                        .name(instName)
                        .id(institutionstilknytning.getInstnr())
                        .roles(roleNames)
                        .build();
            })).distinct().collect(Collectors.toList());
        } catch (https.wsibruger_unilogin_dk.ws.AuthentificationFault authentificationFault) {
            log.error(authentificationFault.getMessage(), authentificationFault);
        }
        return Collections.emptyList();
    }

    public String getFailureUrl(FailureReason reason) {
        return fafnirConf.getFailureRedirect() + "#" + reason.getErrorCode();
    }
}
