package dk.acto.fafnir.api.model.conf;

import lombok.Value;

@Value
public class FafnirConf {
    String url;
    String successRedirect;
    String failureRedirect;

    public String buildUrl(String path) {
        return url + path;
    }
}
