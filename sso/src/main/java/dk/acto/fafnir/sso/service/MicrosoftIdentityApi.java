package dk.acto.fafnir.sso.service;

import com.github.scribejava.core.builder.api.DefaultApi20;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class MicrosoftIdentityApi extends DefaultApi20 {
    private final String tenant;

    @Override
    public String getAccessTokenEndpoint() {
        return "https://login.microsoftonline.com/" + tenant + "/oauth2/v2.0/token";
    }

    @Override
    protected String getAuthorizationBaseUrl() {
        return "https://login.microsoftonline.com/" + tenant + "/oauth2/v2.0/authorize";
    }
}
