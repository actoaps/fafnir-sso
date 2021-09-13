package dk.acto.fafnir.server.model.conf;

import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
public class UniLoginConf {
    String appId;
    String secret;
    String wsUsername;
    String wsPassword;
    boolean singleSignOn;
}
