package dk.acto.fafnir.sso.provider.unilogin;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserInfoResponse {
    @JsonProperty("inst_brugere")
    List<InstitutionTilknytning> instBrugere;
}
