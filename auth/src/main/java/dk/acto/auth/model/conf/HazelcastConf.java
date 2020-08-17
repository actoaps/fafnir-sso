package dk.acto.auth.model.conf;

import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
public class HazelcastConf {
    boolean usernameIsEmail;
    boolean passwordIsEncrypted;

}
