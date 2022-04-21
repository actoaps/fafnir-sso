package dk.acto.fafnir.api.service;

import dk.acto.fafnir.api.model.*;

public interface AdministrationService {
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

    Slice<UserData> readUsers(Long page);

    UserData[] readUsers();

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

    OrganisationData readOrganisation(String providerKey, String providerValue);

    OrganisationData readOrganisation(ProviderMetaData providerMetaData);

    OrganisationData updateOrganisation(OrganisationData source);
    OrganisationData deleteOrganisation(String source);

    ClaimData createClaim(ClaimData source);

    Slice<ClaimData> readClaims(Long page);

    ClaimData readClaims(String orgId, String subject);
    ClaimData updateClaims(ClaimData source);
    ClaimData deleteClaims(ClaimData source);

    OrganisationData[] getOrganisationsForUser(UserData user);

    Slice<UserData> getUsersForOrganisation(String orgId, Long page);

    Slice<OrganisationData> readOrganisations(Long page);

    OrganisationData[] readOrganisations();

    Long countOrganisations();
}
