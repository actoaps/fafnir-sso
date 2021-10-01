package dk.acto.fafnir.api.service;

import dk.acto.fafnir.api.model.ClaimData;
import dk.acto.fafnir.api.model.FafnirUser;
import dk.acto.fafnir.api.model.OrganisationData;
import dk.acto.fafnir.api.model.UserData;

public interface AuthorisationService {
    /**
     * Creates a new user. Fails if user already exists.
     * @param source
     * @return
     */
    UserData createUser(final UserData source);

    /**
     * Reads a user, Fails is user does not exist.
     * @param subject
     * @return
     */
    UserData readUser(final String subject);

    /**
     * ,Updates a user. Fails if user does not exist.
     * @param source
     * @return
     */
    UserData updateUser(final UserData source);

    /**
     * Deletes a user. Fails is user does not exist, or if user has any existing claims.
     * @param subject
     * @return
     */
    UserData deleteUser(final String subject);

    OrganisationData createOrganisation(OrganisationData source);
    OrganisationData readOrganisation(String orgId);
    OrganisationData updateOrganisation(OrganisationData source);
    OrganisationData deleteOrganisation(String source);

    ClaimData createClaim(ClaimData source);
    ClaimData readClaims(String orgId, String subject);
    ClaimData updateClaims(ClaimData source);
    ClaimData deleteClaims(ClaimData source);

    UserData authenticate(String userName, String password);
    FafnirUser authorize (UserData user, String orgId);
    OrganisationData[] getOrganisationsForUser(UserData user);
    UserData[] getUsersForOrganisation(String orgId);
    OrganisationData[] getOrganisations();
}
