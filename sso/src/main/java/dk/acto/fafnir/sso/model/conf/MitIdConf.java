package dk.acto.fafnir.sso.model.conf;

import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
public class MitIdConf {
    String authorityUrl;
    String clientId;
    String secret;
}
