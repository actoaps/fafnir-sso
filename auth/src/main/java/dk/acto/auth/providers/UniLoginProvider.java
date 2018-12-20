package dk.acto.auth.providers;

import dk.acto.auth.ActoConf;
import dk.acto.auth.TokenFactory;
import dk.acto.auth.providers.unilogin.Institution;
import dk.acto.auth.providers.unilogin.UserRole;
import https.uni_login.Instbruger;
import https.uni_login.Institutionstilknytning;
import https.wsibruger_uni_login_dk.ws.WsiBruger;
import https.wsibruger_uni_login_dk.ws.WsiBrugerPortType;
import https.wsiinst_uni_login_dk.ws.AuthentificationFault;
import https.wsiinst_uni_login_dk.ws.WsiInst;
import https.wsiinst_uni_login_dk.ws.WsiInstPortType;
import io.vavr.control.Try;
import lombok.extern.log4j.Log4j2;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


@Log4j2
public class UniLoginProvider {
	private final ActoConf actoConf;
	private final UniLoginService uniloginService;
	private final TokenFactory tokenFactory;

	public UniLoginProvider(ActoConf actoConf, TokenFactory tokenFactory) {
		this.actoConf = actoConf;
		this.uniloginService = Try.of(() -> new UniLoginServiceBuilder(actoConf.getUniLoginAppId())
				.apiSecret(actoConf.getUniLoginSecret())
				.wsUserName(actoConf.getUniLoginWSUsername())
				.wsPassword(actoConf.getUniLoginWSPassword())
				.callbackChooseInstitution(actoConf.getMyUrl() + "/callback-unilogin-choose-organization")
				.callback(actoConf.getMyUrl() + "/callback-unilogin")
				.build()).getOrNull();
		this.tokenFactory = tokenFactory;
	}

	public String authenticate() {
		return uniloginService.getAuthorizationUrl();
	}

	public String callback(String user, String timestamp, String auth) {
		// Validate auth;
		// MD5(timestamp+secret+user)
		boolean validAccess = uniloginService.isValidAccess(user, timestamp, auth);
		if (validAccess) {
			List<Institution> institutionList = this.getInstitutionList(user);
			if (institutionList.size() > 1 || (actoConf.isTestMode() && institutionList.size() > 0)) {
				return uniloginService.getChooseInstitutionUrl(user, timestamp, auth);
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
			https.uni_login.Institution inst = wsiInstPortType.hentInstitution(uniloginService.getWsUsername(), uniloginService.getWsPassword(), institutionId);
			institution = new Institution(inst.getInstnr(), inst.getInstnavn());
		} catch (https.wsiinst_uni_login_dk.ws.AuthentificationFault authentificationFault) {
			authentificationFault.printStackTrace();
			throw new Error("User have no institution");
		}
		return institution;
	}

	private Set<UserRole> getUserRoles(String institutionId, String userId) {
		WsiInst wsiInst = new WsiInst();
		WsiInstPortType wsiInstPortType = wsiInst.getWsiInstPort();
		//AnsatRolle: "Lærer", "Pædagog", "Vikar", "Leder", "Ledelse", "TAP", "Konsulent"
		//EksternRolle: "Ekstern", "Praktikant"
		//ElevRolle: "Barn", "Elev", "Studerende"
		Set<UserRole> roles = new HashSet<>();
		try {
			List<https.uni_login.Instbruger> instbrugerList = wsiInstPortType.hentInstBruger(uniloginService.getWsUsername(), uniloginService.getWsPassword(), institutionId, userId);
			for (Instbruger instbruger : instbrugerList) {
				https.uni_login.Ansat ansat = instbruger.getAnsat();
				https.uni_login.Ekstern ekstern = instbruger.getEkstern();
				https.uni_login.Elev elev = instbruger.getElev();
				if (ansat != null) {
					ansat.getRolle().stream()
							.forEach(ansatrolle -> roles.add(new UserRole(ansatrolle.name(), "EMPLOYEE")));
				}
				if (ekstern != null) {
					roles.add(new UserRole(ekstern.getRolle().name(), "EMP_EXTERNAL"));
				}
				if (elev != null) {
					roles.add(new UserRole(elev.getRolle().name(), "PUPIL"));
				}
			}
		} catch (AuthentificationFault authentificationFault) {
			authentificationFault.printStackTrace();
			throw new Error("User has no rights");
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

		// Validate auth;
		//MD5(timestamp+secret+user)
		boolean validAccess = uniloginService.isValidAccess(userId, timestamp, auth);
		if (validAccess) {
			Set<UserRole> roles = this.getUserRoles(institutionId, userId);
			String[] roleArray = roles.stream().map(UserRole::toString).toArray(String[]::new);
			String jwt = tokenFactory.generateToken(sub, postfixIss, name, orgId, orgName, roleArray);
			return actoConf.getSuccessUrl() + "#" + jwt;
		} else {
			log.error("Authentication failed", "UniLoginProvider");
			return actoConf.getFailureUrl();
		}
	}

	public List<Institution> getInstitutionList(String userId) {
		WsiBruger wsiBruger = new WsiBruger();
		WsiBrugerPortType wsiBrugerPortType = wsiBruger.getWsiBrugerPort();
		List<Institutionstilknytning> institutionstilknytninger = null;

		WsiInst wsiInst = new WsiInst();
		WsiInstPortType wsiInstPortType = wsiInst.getWsiInstPort();
		try {
			institutionstilknytninger = wsiBrugerPortType.hentBrugersInstitutionstilknytninger(uniloginService.getWsUsername(), uniloginService.getWsPassword(), userId);

			return institutionstilknytninger.stream().map((institutionstilknytning -> {
				String instName = "";
				try {
					https.uni_login.Institution inst = wsiInstPortType.hentInstitution(uniloginService.getWsUsername(), uniloginService.getWsPassword(), institutionstilknytning.getInstnr());
					instName = inst.getInstnavn();
				} catch (https.wsiinst_uni_login_dk.ws.AuthentificationFault authentificationFault) {
					authentificationFault.printStackTrace();
				}
				return new Institution(institutionstilknytning.getInstnr(), instName);
			})).distinct().collect(Collectors.toList());
		} catch (https.wsibruger_uni_login_dk.ws.AuthentificationFault authentificationFault) {
			authentificationFault.printStackTrace();
		}
		throw new Error("User have no institution");
	}
}
