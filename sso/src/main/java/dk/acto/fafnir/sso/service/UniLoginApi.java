package dk.acto.fafnir.sso.service;

import com.github.scribejava.core.builder.api.DefaultApi20;

public class UniLoginApi extends DefaultApi20 {

    private static final String BASE_URL = getBaseUrl();

    private static String getBaseUrl() {
        String testEnabled = System.getenv("TEST_ENABLED_UNILOGIN");
        // Default to production if not set, empty, or explicitly "false"
        if (testEnabled != null && !testEnabled.trim().isEmpty() && !testEnabled.trim().equalsIgnoreCase("false")) {
            return "https://et-broker.unilogin.dk/auth/realms/broker";
        }
        // Production endpoint (default)
        return "https://broker.unilogin.dk/auth/realms/broker";
    }

    @Override
    public String getAccessTokenEndpoint() {
        return BASE_URL + "/protocol/openid-connect/token";
    }

    @Override
    protected String getAuthorizationBaseUrl() {
        return BASE_URL + "/protocol/openid-connect/auth";
    }
}

