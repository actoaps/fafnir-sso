package dk.acto.fafnir.model.conf;

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
