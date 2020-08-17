package dk.acto.auth.model.conf;

import dk.acto.auth.providers.validators.GoogleValidator;
import lombok.AllArgsConstructor;
import lombok.Value;

import javax.validation.constraints.NotBlank;

@Value
@AllArgsConstructor
public class GoogleConf {
    @NotBlank
    String appId;
    @NotBlank
    String secret;
}
