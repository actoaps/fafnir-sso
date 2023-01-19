package dk.acto.fafnir.api.model;

import lombok.Builder;
import lombok.Value;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

@Value
@Builder(toBuilder = true)
public class UserData implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    String subject;
    String password;
    String name;
    String metaId;
    String providerOrg;
    Locale locale;
    Instant created;

    public UserData partialUpdate(UserData updated) {
        return UserData.builder()
                .subject(subject)
                .name(Optional.ofNullable(updated.getName()).orElse(name))
                .password(Optional.ofNullable(updated.getPassword()).orElse(password))
                .metaId(Optional.ofNullable(updated.getMetaId()).orElse(metaId))
                .providerOrg(Optional.ofNullable(updated.providerOrg).orElse(providerOrg))
                .locale(Optional.ofNullable(updated.getLocale()).orElse(locale))
                .created(Optional.ofNullable(created)
                        .or(() -> Optional.ofNullable(updated.getCreated()))
                        .orElse(Instant.now()))
                .build();
    }

}
