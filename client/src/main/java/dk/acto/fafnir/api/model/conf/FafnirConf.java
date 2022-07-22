package dk.acto.fafnir.api.model.conf;

import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
public class FafnirConf {
    String url;
    String successRedirect;
    String failureRedirect;
}
