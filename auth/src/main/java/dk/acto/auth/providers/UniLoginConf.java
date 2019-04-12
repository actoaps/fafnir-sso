package dk.acto.auth.providers;

import com.github.scribejava.core.model.ParameterList;
import dk.acto.auth.ActoConf;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;

@Data
public class UniLoginConf {
	@Getter(AccessLevel.NONE)
	private final String authorizationBaseUrl_SingleSignOn = "https://sso.emu.dk/unilogin/login.cgi";
	@Getter(AccessLevel.NONE)
	private final String authorizationBaseUrl_SingleLogin = "http://sli.emu.dk/unilogin/login.cgi";
	private final String apiKey;
	private final String apiSecret;
	private final String callback;
	private final String callbackChooseInstitution;
	private final String wsUsername;
	private final String wsPassword;
	
	@Autowired
	public UniLoginConf(ActoConf actoConf) {
		this.apiKey = actoConf.getUniLoginAppId();
		this.apiSecret = actoConf.getUniLoginSecret();
		this.wsUsername = actoConf.getUniLoginWSUsername();
		this.wsPassword = actoConf.getUniLoginWSPassword();
		this.callbackChooseInstitution = actoConf.getMyUrl() + "/unilogin/org";
		this.callback = actoConf.getMyUrl() + "/unilogin/callback";
	}
	
	private String encodeCallbackUrl() {
		return Base64.encodeBase64String(getCallback().getBytes());
	}
	
	private String fingerprintAuth() {
		return DigestUtils.md5Hex(getCallback() + getApiSecret());
	}
	
	public String getAuthorizationBaseUrl() {
//		return authorizationBaseUrl_SingleSignOn;
		return authorizationBaseUrl_SingleLogin;
	}
	
	public String getChooseInstitutionUrl(String userId, String timestamp, String auth) {
		ParameterList builder = new ParameterList();
		builder.add(UniLoginConstants.USER_ID, userId);
		builder.add(UniLoginConstants.TIMESTAMP, timestamp);
		builder.add(UniLoginConstants.STATE_AUTH_ENCODED, auth);
		return builder.appendTo(getCallbackChooseInstitution());
	}
	
	public String getAuthorizationUrl() {
		ParameterList builder = new ParameterList();
		builder.add(UniLoginConstants.CLIENT_ID, getApiKey());
		builder.add(UniLoginConstants.CLIENT_SECRET, getApiSecret());
		builder.add(UniLoginConstants.CALLBACK, Base64.encodeBase64String(getCallback().getBytes()));
		builder.add(UniLoginConstants.STATE_AUTH_ENCODED, DigestUtils.md5Hex(getCallback() + getApiSecret()));
		return builder.appendTo(getAuthorizationBaseUrl());
	}
	
	/**
	 * Calculation MD5(timestamp+secret+user)
	 *
	 * @param user      name of valid user
	 * @param timestamp YYYYMMDDhhmmss
	 * @param auth      is MD5(timestamp+secret+user)
	 * @return
	 */
	public boolean isValidAccess(String user, String timestamp, String auth) {
		return DigestUtils.md5Hex(timestamp + getApiSecret() + user).equals(auth);
	}
}
