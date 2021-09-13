package dk.acto.fafnir.server.model.conf;

import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
public class AppleConf {
    String appId;
    String secret;
}
