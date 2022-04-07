package dk.acto.fafnir.api.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.Delegate;

import java.io.Serial;
import java.io.Serializable;

@Value
@AllArgsConstructor
@Builder(toBuilder = true)
public class FafnirUser implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Delegate(excludes = Builder.class)
    UserData data;
    String organisationId;
    String organisationName;
    String provider;
    String[] roles;
}
