package dk.acto.auth.providers;

import dk.acto.auth.ActoConf;
import dk.acto.auth.TokenFactory;
import dk.acto.auth.providers.unilogin.Institution;
import https.uni_login.Institutionstilknytning;
import https.wsibruger_uni_login_dk.ws.WsiBruger;
import https.wsibruger_uni_login_dk.ws.WsiBrugerPortType;
import https.wsiinst_uni_login_dk.ws.WsiInst;
import https.wsiinst_uni_login_dk.ws.WsiInstPortType;
import lombok.extern.log4j.Log4j2;

import java.util.List;
import java.util.stream.Collectors;


@Log4j2
public class UniLoginProvider {
	private final ActoConf actoConf;
	private final UniLoginService uniloginService;
	private final TokenFactory tokenFactory;

	public UniLoginProvider(ActoConf actoConf, TokenFactory tokenFactory) {
		this.actoConf = actoConf;
		this.uniloginService = new UniLoginServiceBuilder(actoConf.getUniLoginAppId())
				.apiSecret(actoConf.getUniLoginSecret())
				.wsUserName(actoConf.getUniLoginWSUsername())
				.wsPassword(actoConf.getUniLoginWSPassword())
				.callbackChooseInstitution(actoConf.getMyUrl() + "/callback-unilogin-choose-organization")
				.callback(actoConf.getMyUrl() + "/callback-unilogin")
				.build();
		this.tokenFactory = tokenFactory;
	}

	public String authenticate() {
		return uniloginService.getAuthorizationUrl();
	}

	public String callback(String user, String timestamp, String auth) {
		// Validate auth;
		//MD5(timestamp+secret+user)
		boolean validAccess = uniloginService.isValidAccess(user, timestamp, auth);
		if (validAccess) {
			List<Institution> institutionList = this.getInstitutionList(user);
			if(institutionList.size() == 1) {
				return callbackWithInstitution(user, timestamp, auth, institutionList.get(0).getId());
			}else if(institutionList.size() > 1){
				return uniloginService.getChooseInstitutionUrl(user, timestamp, auth);
			} else {
				log.error("User does not belong to an institution failure", "UniLoginProvider");
				return actoConf.getFailureUrl();
			}
		} else {
			log.error("Authentication failed", "UniLoginProvider");
			return actoConf.getFailureUrl();
		}
	}

	public String callbackWithInstitution(String user, String timestamp, String auth, String institutionId) {
		String displayName = user;

		// Validate auth;
		//MD5(timestamp+secret+user)
		boolean validAccess = uniloginService.isValidAccess(user, timestamp, auth);
		if (validAccess) {
			String jwt = tokenFactory.generateToken(institutionId, "unilogin", displayName);
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
