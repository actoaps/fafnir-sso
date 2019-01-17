package dk.acto.auth.providers;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.client.utils.URIBuilder;

import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

public class UniLoginService {
	private final String authorizationBaseUrl = "https://sso.emu.dk/unilogin/login.cgi";
	private final String authorizationBaseUrlSingleLogin = "http://sli.emu.dk/unilogin/login.cgi";
	private final String apiKey;
	private final String apiSecret;
	private final String callback;
	private final String callbackChooseInstitution;
	private final String wsUsername;
	private final String wsPassword;

	public UniLoginService(String apiKey, String apiSecret, String callback, String callbackChooseInstitution, String wsUsername, String wsPassword) {
		this.apiKey = apiKey;
		this.apiSecret = apiSecret;
		this.callback = callback;
		this.callbackChooseInstitution = callbackChooseInstitution;
		this.wsUsername = wsUsername;
		this.wsPassword = wsPassword;
	}

	public String getAuthorizationBaseUrl() {
		return authorizationBaseUrlSingleLogin;
	}

	public String getApiKey() {
		return apiKey;
	}

	public String getApiSecret() {
		return apiSecret;
	}

	public String getCallback() {
		return callback;
	}

	public String getCallbackChooseInstitution() { return callbackChooseInstitution; }

	public String getWsUsername() {
		return wsUsername;
	}

	public String getWsPassword() {
		return wsPassword;
	}

	private String encodeCallbackUrl() {
		return Base64.encodeBase64String(getCallback().getBytes());
	}

	private String fingerprintAuth() {
		return DigestUtils.md5Hex(getCallback() + getApiSecret());
	}

	public String getChooseInstitutionUrl(String userId, String timestamp, String auth) {
		try {
			URIBuilder builder = new URIBuilder(getCallbackChooseInstitution())
					.addParameter("user", userId)
					.addParameter("timestamp", timestamp)
					.addParameter("auth", auth)
					.setCharset(StandardCharsets.UTF_8);
			return String.valueOf(builder.build());
		} catch (URISyntaxException e) {
			// Will never be reached, with a valid Authorization Url.
			throw new Error("Wrong Base Url");
		}
	}

	public String getAuthorizationUrl() {
		try {
			URIBuilder builder = new URIBuilder(getAuthorizationBaseUrl())
					.addParameter("id", getApiKey())
					.addParameter("secret", getApiSecret())
					.addParameter("path", Base64.encodeBase64String(getCallback().getBytes()))
					.addParameter("auth", DigestUtils.md5Hex(getCallback() + getApiSecret()))
					.setCharset(StandardCharsets.UTF_8);
			return String.valueOf(builder.build());
		} catch (URISyntaxException e) {
			// Will never be reached, with a valid Authorization Url.
			throw new Error("Wrong Base Url");
		}
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
