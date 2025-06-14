package dk.acto.fafnir.sso.provider.economic;

import lombok.Data;

@Data
public class EconomicCustomer {
    private String customerNumber;
    private String currency;
    private String email;
    private String name;
    private Boolean barred;
}
