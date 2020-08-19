package dk.acto.fafnir.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.io.Serializable;
import java.util.List;
import java.util.Locale;

@Value
@AllArgsConstructor
@Builder(toBuilder = true)
public class FafnirUser implements Serializable {
    private static final long serialVersionUID = 1L;

    String subject;
    String password;
    String provider;
    String name;
    String metaId;
    Locale locale;
    String organisationId;
    String organisationName;
    List<String> roles;
}
