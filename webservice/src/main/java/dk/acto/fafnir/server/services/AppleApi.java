package dk.acto.fafnir.server.services;

import com.github.scribejava.core.builder.api.DefaultApi20;

public class AppleApi extends DefaultApi20 {
    @Override
    public String getAccessTokenEndpoint() {
        return "https://appleid.apple.com/auth/token";
    }

    @Override
    protected String getAuthorizationBaseUrl() {
        return "https://appleid.apple.com/auth/authorize";
    }
}
