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
    OrganisationData readOrganisation(TennantIdentifier identifier);
    OrganisationData updateOrganisation(OrganisationData source);
    OrganisationData deleteOrganisation(String source);

    ClaimData createClaim(final OrganisationSubjectPair pair, final ClaimData source);
    ClaimData readClaims(final OrganisationSubjectPair pair);
    ClaimData updateClaims(final OrganisationSubjectPair pair, final ClaimData source);
    ClaimData deleteClaims(OrganisationSubjectPair pair);

    OrganisationData[] getOrganisationsForUser(String user);
    UserData[] getUsersForOrganisation(String orgId);
    Slice<OrganisationData> readOrganisations(Long page);
    OrganisationData[] readOrganisations();
    Long countOrganisations();
}
