package dk.acto.fafnir.server.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class UserData {
    private final String email;
    private final String name;
    private final String userId;

}
