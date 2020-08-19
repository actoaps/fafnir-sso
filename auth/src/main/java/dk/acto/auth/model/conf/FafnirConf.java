package dk.acto.auth.model.conf;

import lombok.AllArgsConstructor;
import lombok.Value;
import org.hibernate.validator.constraints.URL;

import javax.validation.constraints.NotBlank;

@Value
@AllArgsConstructor
public class FafnirConf {
    @NotBlank
    @URL
    String url;

    @NotBlank
    @URL
    String successRedirect;

    @NotBlank
    @URL
    String failureRedirect;
}
