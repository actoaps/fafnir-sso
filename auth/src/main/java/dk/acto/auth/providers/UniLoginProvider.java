package dk.acto.auth.providers;

import dk.acto.auth.ActoConf;
import dk.acto.auth.TokenFactory;
import dk.acto.auth.providers.unilogin.Institution;
import dk.acto.auth.providers.unilogin.UserRole;
import https.uni_login.Institutionstilknytning;
import https.wsibruger_uni_login_dk.ws.WsiBruger;
import https.wsibruger_uni_login_dk.ws.WsiBrugerPortType;
import https.wsiinst_uni_login_dk.ws.WsiInst;
import https.wsiinst_uni_login_dk.ws.WsiInstPortType;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


@Log4j2
@Component
public class UniLoginProvider {
	private final ActoConf actoConf;
	private final UniLoginConf uniloginConf;
	private final TokenFactory tokenFactory;

	@Autowired
	public UniLoginProvider(ActoConf actoConf, UniLoginConf uniloginConf, TokenFactory tokenFactory) {
		this.actoConf = actoConf;
		this.uniloginConf = uniloginConf;
		this.tokenFactory = tokenFactory;
	}

	public String authenticate() {
		return uniloginConf.getAuthorizationUrl();
	}

	public String callback(String user, String timestamp, String auth) {
		boolean validAccess = uniloginConf.isValidAccess(user, timestamp, auth);
		if (validAccess) {
			List<Institution> institutionList = this.getInstitutionList(user);
			if (institutionList.size() > 1 || (actoConf.isTestMode() && !institutionList.isEmpty())) {
				return uniloginConf.getChooseInstitutionUrl(user, timestamp, auth);
			} else if (institutionList.size() == 1) {
				return callbackWithInstitution(user, timestamp, auth, institutionList.get(0).getId());
			} else {
				log.error("User does not belong to an institution failure", "UniLoginProvider");
				return actoConf.getFailureUrl();
			}
		} else {
			log.error("Authentication failed", "UniLoginProvider");
			return actoConf.getFailureUrl();
		}
	}

	private Institution getInstitutionFromId(String institutionId) {
		WsiInst wsiInst = new WsiInst();
		WsiInstPortType wsiInstPortType = wsiInst.getWsiInstPort();
		Institution institution;
		try {
			https.uni_login.Institution inst = wsiInstPortType.hentInstitution(uniloginConf.getWsUsername(), uniloginConf.getWsPassword(), institutionId);
			institution = new Institution(inst.getInstnr(), inst.getInstnavn());
		} catch (https.wsiinst_uni_login_dk.ws.AuthentificationFault authentificationFault) {
			log.error(authentificationFault.getMessage(), authentificationFault);
			throw new Error("User have no institution");
		}
		return institution;
	}

	private Set<UserRole> getUserRoles(String institutionId, String userId) {
		WsiBruger wsiBruger = new WsiBruger();
		WsiBrugerPortType wsiBrugerPortType = wsiBruger.getWsiBrugerPort();
		Set<UserRole> roles;
		try {
			java.util.List<https.uni_login.Institutionstilknytning> institutionstilknytninger = wsiBrugerPortType.hentBrugersInstitutionstilknytninger(uniloginConf.getWsUsername(), uniloginConf.getWsPassword(), userId);
			institutionstilknytninger = institutionstilknytninger.stream()
					.filter(til->til.getInstnr() == institutionId)
					.collect(Collectors.toList());
			roles = toUserRoles(institutionstilknytninger);
		} catch (https.wsibruger_uni_login_dk.ws.AuthentificationFault authentificationFault) {
			log.error(authentificationFault.getMessage(), authentificationFault);
			throw new Error("User has no rights");
		}
		return roles;
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
	private Set<UserRole> toUserRoles(java.util.List<https.uni_login.Institutionstilknytning> institutionstilknytninger) {
		Set<UserRole> roles = new HashSet<>();
		for (Institutionstilknytning institutionstilknytning : institutionstilknytninger) {
			https.uni_login.InstitutionstilknytningAnsat ansat = institutionstilknytning.getAnsat();
			https.uni_login.InstitutionstilknytningEkstern ekstern = institutionstilknytning.getEkstern();
			https.uni_login.InstitutionstilknytningElev elev = institutionstilknytning.getElev();
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
		final String sub = userId; // jwt:sub == UniLogin username
		final String postfixIss = "unilogin"; // jwt:iss ends as prefix-postfix, example fafnir-unilogin
		String name = getUserFullNameFromId(userId); //jwt:name, full name of user
		final String orgId = institutionId; // jwt:org_id, the organisation id of the user
		final String orgName = getInstitutionFromId(institutionId).getName(); // jwt:org_name, the organisation name of the user

		boolean validAccess = uniloginConf.isValidAccess(userId, timestamp, auth);
		if (validAccess) {
			Set<UserRole> roles = this.getUserRoles(institutionId, userId);
			String[] roleArray = roles.stream().map(UserRole::toString).toArray(String[]::new);
			String jwt = tokenFactory.generateToken(sub, postfixIss, name, orgId, orgName, roleArray);
			return actoConf.getSuccessUrl() + (actoConf.isEnableParameter() ? "?jwtToken=" : "#") + jwt;
		} else {
			log.error("Authentication failed", "UniLoginProvider");
			return actoConf.getFailureUrl();
		}
	}

	public List<Institution> getInstitutionList(String userId) {
		WsiBruger wsiBruger = new WsiBruger();
		WsiBrugerPortType wsiBrugerPortType = wsiBruger.getWsiBrugerPort();
		List<Institutionstilknytning> institutionstilknytninger;

		WsiInst wsiInst = new WsiInst();
		WsiInstPortType wsiInstPortType = wsiInst.getWsiInstPort();
		try {
			institutionstilknytninger = wsiBrugerPortType.hentBrugersInstitutionstilknytninger(uniloginConf.getWsUsername(), uniloginConf.getWsPassword(), userId);
			return institutionstilknytninger.stream().map((institutionstilknytning -> {
				String instName = "";
				try {
					https.uni_login.Institution inst = wsiInstPortType.hentInstitution(uniloginConf.getWsUsername(), uniloginConf.getWsPassword(), institutionstilknytning.getInstnr());
					instName = inst.getInstnavn();
				} catch (https.wsiinst_uni_login_dk.ws.AuthentificationFault authentificationFault) {
					log.error(authentificationFault.getMessage(), authentificationFault);
				}
				List<String> roleNames = toUserRoles(institutionstilknytninger).stream().map(UserRole::getName).collect(Collectors.toList());
				return new Institution(institutionstilknytning.getInstnr(), instName, roleNames);
			})).distinct().collect(Collectors.toList());
		} catch (https.wsibruger_uni_login_dk.ws.AuthentificationFault authentificationFault) {
			log.error(authentificationFault.getMessage(), authentificationFault);
		}
		throw new Error("User have no institution");
	}
}
