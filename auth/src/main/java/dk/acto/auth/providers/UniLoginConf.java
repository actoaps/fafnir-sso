package dk.acto.auth.providers;

import dk.acto.auth.ActoConf;
import lombok.Data;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriBuilderFactory;

@Data
public class UniLoginConf {
	private final String authorizationBaseUrl = "https://sso.emu.dk/unilogin/login.cgi";
	private final String authorizationBaseUrlSingleLogin = "http://sli.emu.dk/unilogin/login.cgi";
	private final String apiKey;
	private final String apiSecret;
	private final String callback;
	private final String callbackChooseInstitution;
	private final String wsUsername;
	private final String wsPassword;
	private final UriBuilderFactory factory;


	@Autowired
	public UniLoginConf(UriBuilderFactory factory, ActoConf actoConf) {
		this.factory = factory;
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

	public String getChooseInstitutionUrl(String userId, String timestamp, String auth) {
	    return factory.builder().pathSegment(getCallbackChooseInstitution())
				.queryParam("user", userId)
				.queryParam("timestamp", timestamp)
				.queryParam("auth", auth)
                .toString();
	}

	public String getAuthorizationUrl() {
		return factory.builder().pathSegment(getAuthorizationBaseUrl())
                .queryParam("id", getApiKey())
                .queryParam("secret", getApiSecret())
                .queryParam("path", Base64.encodeBase64String(getCallback().getBytes()))
                .queryParam("auth", DigestUtils.md5Hex(getCallback() + getApiSecret()))
                .toString();
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
