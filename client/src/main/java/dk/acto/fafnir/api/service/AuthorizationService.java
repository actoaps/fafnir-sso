package dk.acto.fafnir.api.service;

import dk.acto.fafnir.api.model.FafnirUser;
import dk.acto.fafnir.api.model.UserData;

public interface AuthorizationService {
    UserData authenticate(String userName, String password, boolean isEncrypted);
    FafnirUser authorize (UserData user, String orgId);
}
