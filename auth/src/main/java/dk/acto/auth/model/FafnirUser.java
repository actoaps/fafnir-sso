package dk.acto.auth.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Locale;

@Value
@AllArgsConstructor
@Builder
public class FafnirUser {
    String subject;
    String passwordHash;
    String provider;
    String name;
    String metaId;
    Locale locale;
    String organisationId;
    String organisationName;
    List<String> roles;
}
