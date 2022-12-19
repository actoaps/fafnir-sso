package dk.acto.fafnir.api.model.conf;

import lombok.Value;

@Value
public class HazelcastConf {
    boolean usernameIsEmail;
    boolean passwordIsEncrypted;
    boolean trimUsername;
    String prefix;
}
