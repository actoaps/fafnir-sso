package dk.acto.fafnir.server.service;

import com.github.scribejava.core.builder.api.DefaultApi20;
import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@EqualsAndHashCode(callSuper = true)
public class MitIdApi extends DefaultApi20 {
    String authorityUrl;

    @Override
    public String getAccessTokenEndpoint() {
        return authorityUrl + "/connect/token";
    }

    @Override
    protected String getAuthorizationBaseUrl() {
        return authorityUrl + "/connect/authorize";
    }
}

