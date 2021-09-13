package dk.acto.fafnir.server.model.conf;

import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
public class MitIdConf {
    String authorityUrl;
    String clientId;
    String secret;
}
