package dk.acto.fafnir.api.service;

import dk.acto.fafnir.api.model.ClaimData;

public interface AuthenticationService {
    ClaimData authenticate(String organisation, String username, String password);
}
