package dk.acto.fafnir.api;

import dk.acto.fafnir.api.model.ClaimsPayload;

public interface ClaimConsumer {
    ClaimsPayload consume(ClaimsPayload payload);
    ClaimsPayload destroy(ClaimsPayload payload);
}
