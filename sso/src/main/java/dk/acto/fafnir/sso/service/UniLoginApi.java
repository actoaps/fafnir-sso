package dk.acto.fafnir.sso.service;

import com.github.scribejava.core.builder.api.DefaultApi20;

public class UniLoginApi extends DefaultApi20 {

    private static final String BASE_URL = "https://et-broker.unilogin.dk/auth/realms/broker";

    @Override
    public String getAccessTokenEndpoint() {
        return BASE_URL + "/protocol/openid-connect/token";
    }

    @Override
    protected String getAuthorizationBaseUrl() {
        return BASE_URL + "/protocol/openid-connect/auth";
    }
}

