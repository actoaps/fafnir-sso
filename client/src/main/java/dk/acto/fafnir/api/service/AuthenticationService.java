package dk.acto.fafnir.api.service;

import dk.acto.fafnir.api.model.FafnirUser;

public interface AuthenticationService {
    FafnirUser authenticate(String organisation, String username, String password);
}
