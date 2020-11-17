package dk.acto.fafnir.model.conf;

import lombok.AllArgsConstructor;
import lombok.Value;

import javax.validation.constraints.NotBlank;

@Value
@AllArgsConstructor
public class UniLoginConf {
    String appId;
    String secret;
    String wsUsername;
    String wsPassword;
    boolean singleSignOn;
}
