package dk.acto.auth.providers;

import com.github.scribejava.core.utils.Preconditions;

public class UniLoginServiceBuilder {
	private String callback;
	private String callbackChooseInstitution;
	private String apiKey;
	private String apiSecret;
	private String wsUsername;
	private String wsPassword;

	public UniLoginServiceBuilder(String apiKey) {
		apiKey(apiKey);
	}

	/**
	 * Adds a callback url
	 *
	 * @param callback callback url. Must be a valid url or 'oob'
	 *                 ({@link com.github.scribejava.core.model.OAuthConstants#OOB} for out of band OAuth
	 * @return the {@link UniLoginServiceBuilder} instance for method chaining
	 */
	public UniLoginServiceBuilder callback(String callback) {
		this.callback = callback;
		return this;
	}

	/**
	 *
	 * @param callbackChooseInstitution url.
	 * @return the {@link UniLoginServiceBuilder} instance for method chaining
	 */
	public UniLoginServiceBuilder callbackChooseInstitution(String callbackChooseInstitution) {
		this.callbackChooseInstitution = callbackChooseInstitution;
		return this;
	}

	/**
	 * Configures the api key
	 *
	 * @param apiKey The api key for your application
	 * @return the {@link UniLoginServiceBuilder} instance for method chaining
	 */
	public final UniLoginServiceBuilder apiKey(String apiKey) {
		Preconditions.checkEmptyString(apiKey, "Invalid Api key");
		this.apiKey = apiKey;
		return this;
	}

	/**
	 * Configures the api secret
	 *
	 * @param apiSecret The api secret for your application
	 * @return the {@link UniLoginServiceBuilder} instance for method chaining
	 */
	public UniLoginServiceBuilder apiSecret(String apiSecret) {
		Preconditions.checkEmptyString(apiSecret, "Invalid Api secret");
		this.apiSecret = apiSecret;
		return this;
	}

	/**
	 *
	 * @param wsUsername
	 * @return
	 */
	public UniLoginServiceBuilder wsUserName(String wsUsername) {
		Preconditions.checkEmptyString(wsUsername, "Invalid Webservice Username");
		this.wsUsername = wsUsername;
		return this;
	}

	/**
	 *
	 * @param wsPassword
	 * @return
	 */
	public UniLoginServiceBuilder wsPassword(String wsPassword) {
		Preconditions.checkEmptyString(wsUsername, "Invalid Webservice Password");
		this.wsPassword = wsPassword;
		return this;
	}

	/**
	 * Returns the fully configured {@link UniLoginService}
	 *
	 * @return fully configured {@link UniLoginService}
	 */
	public UniLoginService build() {
		return new UniLoginService(apiKey, apiSecret, callback, callbackChooseInstitution, wsUsername, wsPassword);
	}
}
