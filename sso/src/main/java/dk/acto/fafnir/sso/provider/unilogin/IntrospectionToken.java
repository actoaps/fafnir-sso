package dk.acto.fafnir.sso.provider.unilogin;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class IntrospectionToken {
    int exp;
    int iat;
    int auth_time;
    String jti;
    String iss;
    String sub;
    String typ;
    String azp;
    String session_state;
    String nonce;
    String acr;
    String scope;
    String spec_ver;
    String unilogin_loa;
    String aktoer_gruppe;
    String loa;
    String uniid;
    String client_id;
}
