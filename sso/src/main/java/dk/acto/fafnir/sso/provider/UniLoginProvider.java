package dk.acto.fafnir.sso.provider;

import dk.acto.fafnir.api.model.*;
import dk.acto.fafnir.api.model.FailureReason;
import dk.acto.fafnir.sso.util.TokenFactory;
import dk.acto.fafnir.api.model.conf.FafnirConf;
import dk.acto.fafnir.sso.model.conf.TestConf;
import dk.acto.fafnir.sso.provider.unilogin.Institution;
import dk.acto.fafnir.sso.provider.unilogin.UserRole;
import https.unilogin.Institutionstilknytning;
import https.wsibruger_unilogin_dk.ws.WsiBruger;
import https.wsibruger_unilogin_dk.ws.WsiBrugerPortType;
import https.wsiinst_unilogin_dk.ws.WsiInst;
import https.wsiinst_unilogin_dk.ws.WsiInstPortType;
import io.vavr.control.Try;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;


@Log4j2
@Component
@AllArgsConstructor
@ConditionalOnBean(UniLoginHelper.class)
public class UniLoginProvider {
    private final FafnirConf fafnirConf;
    private final UniLoginHelper uniloginHelper;
    private final TokenFactory tokenFactory;
    private final Optional<TestConf> testConf;

    public String authenticate() {
        return uniloginHelper.getAuthorizationUrl();
    }

    public String callback(String user, String timestamp, String auth) {
        boolean validAccess = uniloginHelper.isValidAccess(user, timestamp, auth);
        if (validAccess) {
            List<Institution> institutionList = Try.of(() -> this.getInstitutionList(user)).getOrElse(Collections.emptyList());
            if (institutionList.size() > 1 || (testConf.isPresent() && !institutionList.isEmpty())) {
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
        WsiInst wsiInst = new WsiInst();
        WsiInstPortType wsiInstPortType = wsiInst.getWsiInstPort();
        try {
            https.unilogin.Institution inst = wsiInstPortType.hentInstitution(uniloginHelper.getWsUsername(), uniloginHelper.getWsPassword(), institutionId);
            return Optional.of(new Institution(inst.getInstnr(), inst.getInstnavn()));
        } catch (https.wsiinst_unilogin_dk.ws.AuthentificationFault authentificationFault) {
            log.error(authentificationFault.getMessage(), authentificationFault);
            return Optional.empty();
        }
    }

    private Set<UserRole> getUserRoles(String institutionId, String userId) {
        WsiBruger wsiBruger = new WsiBruger();
        WsiBrugerPortType wsiBrugerPortType = wsiBruger.getWsiBrugerPort();
        try {
            java.util.List<https.unilogin.Institutionstilknytning> institutionstilknytninger = wsiBrugerPortType.hentBrugersInstitutionstilknytninger(uniloginHelper.getWsUsername(), uniloginHelper.getWsPassword(), userId);
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
        for (Institutionstilknytning institutionstilknytning : institutionstilknytninger) {
            https.unilogin.InstitutionstilknytningAnsat ansat = institutionstilknytning.getAnsat();
            https.unilogin.InstitutionstilknytningEkstern ekstern = institutionstilknytning.getEkstern();
            https.unilogin.InstitutionstilknytningElev elev = institutionstilknytning.getElev();
            if (ansat != null) {
                ansat.getRolle()
                        .forEach(ansatrolle -> roles.add(new UserRole(ansatrolle.name(), "EMPLOYEE")));
            }
            if (ekstern != null) {
                roles.add(new UserRole(ekstern.getRolle().name(), "EMP_EXTERNAL"));
            }
            if (elev != null) {
                roles.add(new UserRole(elev.getRolle().name(), "PUPIL"));
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
        String name = getUserFullNameFromId(userId); //jwt:name, full name of user
        final String orgName = getInstitutionFromId(institutionId).map(Institution::getName).orElseThrow(() -> new RuntimeException("No institution")); // jwt:org_name, the organisation name of the user

        boolean validAccess = uniloginHelper.isValidAccess(userId, timestamp, auth);
        if (validAccess) {
            Set<UserRole> roles = this.getUserRoles(institutionId, userId);

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

            String jwt = tokenFactory.generateToken(subjectActual, orgActual, claimsActual, ProviderMetaData.builder()
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
        WsiBruger wsiBruger = new WsiBruger();
        WsiBrugerPortType wsiBrugerPortType = wsiBruger.getWsiBrugerPort();
        List<Institutionstilknytning> institutionstilknytninger;

        WsiInst wsiInst = new WsiInst();
        WsiInstPortType wsiInstPortType = wsiInst.getWsiInstPort();
        try {
            institutionstilknytninger = wsiBrugerPortType.hentBrugersInstitutionstilknytninger(uniloginHelper.getWsUsername(), uniloginHelper.getWsPassword(), userId);
            return institutionstilknytninger.stream().map((institutionstilknytning -> {
                String instName = "";
                try {
                    https.unilogin.Institution inst = wsiInstPortType.hentInstitution(uniloginHelper.getWsUsername(), uniloginHelper.getWsPassword(), institutionstilknytning.getInstnr());
                    instName = inst.getInstnavn();
                } catch (https.wsiinst_unilogin_dk.ws.AuthentificationFault authentificationFault) {
                    log.error(authentificationFault.getMessage(), authentificationFault);
                }
                List<String> roleNames = toUserRoles(institutionstilknytninger).stream().map(UserRole::toString).collect(Collectors.toList());
                return new Institution(institutionstilknytning.getInstnr(), instName, roleNames);
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
