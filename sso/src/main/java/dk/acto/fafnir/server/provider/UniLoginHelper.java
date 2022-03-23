package dk.acto.fafnir.server.provider;

import com.github.scribejava.core.model.ParameterList;
import dk.acto.fafnir.server.model.conf.FafnirConf;
import dk.acto.fafnir.server.model.conf.UniLoginConf;
import lombok.Data;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Data
@Component
@ConditionalOnBean(UniLoginConf.class)
public class UniLoginHelper {
	public static final String USER_ID = "user";
	public static final String TIMESTAMP = "timestamp";

	public static final String CLIENT_ID = "id";
	public static final String CLIENT_SECRET = "secret";
	public static final String PATH = "path";
	public static final String STATE_AUTH_ENCODED = "auth";

	private static final String AUTHORIZATION_BASEURL_SINGLE_SIGN_ON = "https://sso.emu.dk/unilogin/login.cgi";
	private static final String AUTHORIZATION_BASEURL_SINGLE_LOGIN = "http://sli.emu.dk/unilogin/login.cgi";

	private final String apiKey;
	private final String apiSecret;
	private final String callback;
	private final String callbackChooseInstitution;
	private final String wsUsername;
	private final String wsPassword;
	private final boolean useSingleSignOn;

	public UniLoginHelper(UniLoginConf uniLoginConf, FafnirConf fafnirConf) {
		this.apiKey = uniLoginConf.getAppId();
		this.apiSecret = uniLoginConf.getSecret();
		this.wsUsername = uniLoginConf.getWsUsername();
		this.wsPassword = uniLoginConf.getWsPassword();
		this.useSingleSignOn = uniLoginConf.isSingleSignOn();
		this.callbackChooseInstitution = fafnirConf.getUrl() + "/unilogin/org";
		this.callback = fafnirConf.getUrl() + "/unilogin/callback";
	}

	public String getAuthorizationBaseUrl() {
		return useSingleSignOn ?
				AUTHORIZATION_BASEURL_SINGLE_SIGN_ON :
				AUTHORIZATION_BASEURL_SINGLE_LOGIN;
	}

	public String getChooseInstitutionUrl(String userId, String timestamp, String auth) {
		ParameterList builder = new ParameterList();
		builder.add(USER_ID, userId);
		builder.add(TIMESTAMP, timestamp);
		builder.add(STATE_AUTH_ENCODED, auth);
		return builder.appendTo(getCallbackChooseInstitution());
	}

	public String getAuthorizationUrl() {
		ParameterList builder = new ParameterList();
		builder.add(CLIENT_ID, getApiKey());
		builder.add(CLIENT_SECRET, getApiSecret());
		builder.add(PATH, Base64.encodeBase64String(getCallback().getBytes()));
		builder.add(STATE_AUTH_ENCODED, DigestUtils.md5Hex(getCallback() + getApiSecret()));
		return builder.appendTo(getAuthorizationBaseUrl());
	}

	public boolean isValidAccess(String user, String timestamp, String auth) {
		return DigestUtils.md5Hex(timestamp + getApiSecret() + user).equals(auth);
	}
}
