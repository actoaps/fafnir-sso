package dk.acto.fafnir.sso.model.conf;

import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
public class FacebookConf {
    String appId;
    String secret;
}