package dk.acto.auth.model.conf;

import dk.acto.auth.providers.validators.UniLoginValidator;
import lombok.AllArgsConstructor;
import lombok.Value;

import javax.validation.constraints.NotBlank;

@Value
@AllArgsConstructor
public class UniLoginConf {
    @NotBlank
    String appId;
    @NotBlank
    String secret;
    @NotBlank
    String wsUsername;
    @NotBlank
    String wsPassword;
    boolean singleSignOn;
}
