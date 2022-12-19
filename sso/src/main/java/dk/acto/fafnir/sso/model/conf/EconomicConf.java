package dk.acto.fafnir.sso.model.conf;

import lombok.Value;

@Value
public class EconomicConf {
    String appSecretToken;
    String agreementGrantToken;
}
