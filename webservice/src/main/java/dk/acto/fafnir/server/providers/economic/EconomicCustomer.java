package dk.acto.fafnir.server.providers.economic;

import lombok.Data;

@Data
public class EconomicCustomer {
    private String customerNumber;
    private String currency;
    private String email;
    private String name;

    public static class CustomerWrapper extends CollectionWrapper<EconomicCustomer>{
    }
}
