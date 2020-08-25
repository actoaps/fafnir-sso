package dk.acto.fafnir.model.conf;

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
