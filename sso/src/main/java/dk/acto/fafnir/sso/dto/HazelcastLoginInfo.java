package dk.acto.fafnir.sso.dto;

import lombok.Value;

@Value
public class HazelcastLoginInfo {
    String email;
    String password;
    String orgId;
}
