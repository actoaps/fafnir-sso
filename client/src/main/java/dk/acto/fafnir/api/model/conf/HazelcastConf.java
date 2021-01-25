package dk.acto.fafnir.api.model.conf;

import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
public class HazelcastConf {
    boolean usernameIsEmail;
    boolean passwordIsEncrypted;
    boolean trimUsername;
    String mapName;
}
