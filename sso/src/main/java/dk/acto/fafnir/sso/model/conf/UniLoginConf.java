package dk.acto.fafnir.sso.model.conf;

import lombok.Value;

@Value
public class UniLoginConf {
    String appId;
    String secret;
    String wsUsername;
    String wsPassword;
    boolean singleSignOn;
}
