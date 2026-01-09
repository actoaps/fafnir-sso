package dk.acto.fafnir.sso.provider.unilogin;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class InstitutionTilknytning {
    @JsonProperty("in_tnr")
    String inTnr;  // Institution number

    @JsonProperty("in_tnavn")
    String inTnavn; // Institution name

    // Fields for roles, as they might appear in UserInfo response
    // Format 1: Direct "roller" array with strings like "PÆDAGOG@EMPLOYEE"
    List<String> roller;

    // Format 2: Separate arrays for different role types
    @JsonProperty("ansat_roller")
    List<String> ansatRoller;
    @JsonProperty("elev_roller")
    List<String> elevRoller;
    @JsonProperty("ekstern_roller")
    List<String> eksternRoller;
}
