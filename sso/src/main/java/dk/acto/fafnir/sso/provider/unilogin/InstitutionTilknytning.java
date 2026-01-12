package dk.acto.fafnir.sso.provider.unilogin;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class InstitutionTilknytning {
    @JsonProperty("in_tnr")
    public String inTnr;  // Institution number

    @JsonProperty("in_tnavn")
    public String inTnavn; // Institution name

    // Fields for roles, as they might appear in UserInfo response
    // Format 1: Direct "roller" array with strings like "PÆDAGOG@EMPLOYEE"
    @JsonProperty("roller")
    public List<String> roller;

    // Format 2: Separate arrays for different role types
    @JsonProperty("ansat_roller")
    public List<String> ansatRoller;
    
    @JsonProperty("elev_roller")
    public List<String> elevRoller;
    
    @JsonProperty("ekstern_roller")
    public List<String> eksternRoller;
}
