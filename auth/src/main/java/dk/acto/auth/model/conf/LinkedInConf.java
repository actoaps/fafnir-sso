package dk.acto.auth.model.conf;

import dk.acto.auth.providers.validators.LinkedInValidator;
import lombok.AllArgsConstructor;
import lombok.Value;

import javax.validation.constraints.NotBlank;

@Value
@AllArgsConstructor
public class LinkedInConf {
    @NotBlank
    String appId;
    @NotBlank
    String secret;
}
