package dk.acto.fafnir.api.service;

import dk.acto.fafnir.api.model.*;
import reactor.core.publisher.ConnectableFlux;


public interface AdministrationService {
    /**
     * Creates a new user. Fails if user already exists.
     *
     * @param source the UserData for the new user.
     * @return the new UserData.
     */
    UserData createUser(final UserData source);

    /**
     * Reads a user, Fails is user does not exist.
     *
     * @param subject the user's subject.
     * @return the UserData for the user.
     */
    UserData readUser(final String subject);

    /**
     * Read a paginated list of users. Page size is defined by Slice.PAGE_SIZE (default 30).
     *
     * @param page the page to read. 0 indexed.
     * @return page with users.
     */
    Slice<UserData> readUsers(Long page);

    /**
     * Reads all users.
     *
     * @return an array with all users.
     */
    UserData[] readUsers();

    /**
     * Updates a user. Fails if user does not exist.
     *
     * @param source the user's new values.
     * @return the new user's UserData.
     */
    UserData updateUser(final UserData source);

    /**
     * Deletes a user. Fails is user does not exist, or if user has any existing claims.
     *
     * @param subject the user's subject.
     * @return the UserData for the deleted user.
     */
    UserData deleteUser(final String subject);

    /**
     * Creates a new organisation. Fails if organisation already exists.
     *
     * @param source the OrganisationData for the new organisation.
     * @return the created organisatian's OrganisationData.
     */
    OrganisationData createOrganisation(OrganisationData source);

    /**
     * Reads an organisation. Fails is organisation does not exist.
     *
     * @param orgId the organisation id.
     * @return the organisation's OrganisationData.
     */
    OrganisationData readOrganisation(String orgId);

    /**
     * Read a paginated list of organisations. Page size is defined by Slice.PAGE_SIZE (default 30).
     *
     * @param page the page to read. 0 indexed.
     * @return page with organisations.
     */
    Slice<OrganisationData> readOrganisations(Long page);

    /**
     * Reads all organisations.
     *
     * @return an array with all organisations.
     */
    OrganisationData[] readOrganisations();

    /**
     * Reads the amount of organisations.
     *
     * @return the number of organisations.
     */
    Long countOrganisations();

    /**
     * Reads an organisation by TenantIdentifier. Fails if organisation does not exist.
     * Will return the one of any matches, order is not guaranteed.
     *
     * @param identifier the matcher function.
     * @return the organisation's OrganisationData.
     */
    OrganisationData readOrganisation(TenantIdentifier identifier);

    /**
     * Updates an organisation. Fails if organisation does not exist.
     *
     * @param source the organisation's new values.
     * @return the new organisation's OrganisationData.
     */
    OrganisationData updateOrganisation(OrganisationData source);

    /**
     * Deletes an organisation. Fails is organisation does not exist.
     *
     * @param orgId the organisation's id.
     * @return the OrganisationData for the deleted organisation.
     */
    OrganisationData deleteOrganisation(String orgId);

    /**
     * Creates a claim for a user and an organisation. Fails if user or organisation does not exist, or if claim
     * already exists.
     *
     * @param pair   the user - organisation pair.
     * @param source the ClaimData for the new claim.
     * @return the created ClaimData.
     */
    ClaimData createClaim(final OrganisationSubjectPair pair, final ClaimData source);

    /**
     * Reads all claims for an organisation - user pair. Fails if pair doesn't have any claims.
     *
     * @param pair the user - organisation pair.
     * @return the pair's ClaimData.
     */
    ClaimData readClaims(final OrganisationSubjectPair pair);

    /**
     * Update claims for an organisation - user pair. Fails if pair doesn't have any claims.
     *
     * @param pair   the user - organisation pair.
     * @param source the ClaimData to replace the old claim.
     * @return the updated ClaimData.
     */
    ClaimData updateClaims(final OrganisationSubjectPair pair, final ClaimData source);

    /**
     * Deletes all claims for an organisation - user pair. Fails if pair doesn't have any claims.
     *
     * @param pair the user - organisation pair.
     * @return the deleted claims.
     */
    ClaimData deleteClaims(OrganisationSubjectPair pair);

    /**
     * Reads all organisations for a user. A user is only in an organisation if there exists a claim for that
     * organisation - user pair.
     *
     * @param subject the user's subject.
     * @return the organisations which the user is part of.
     */
    OrganisationData[] getOrganisationsForUser(String subject);

    /**
     * Reads all users for an organisation. A user is only in an organisation if there exists a claim for that
     * organisation - user pair.
     *
     * @param orgId the user's subject.
     * @return the users which are part of the organisation.
     */
    UserData[] getUsersForOrganisation(String orgId);

    /**
     * Gets a ConnectableFlux which produces with every new user created. The Flux does not complete.
     *
     * @return the ConnectableFlux.
     */
    ConnectableFlux<UserData> getUserFlux();

    /**
     * Gets a ConnectableFlux which produces with every new organisation created. The Flux does not complete.
     *
     * @return the ConnectableFlux.
     */
    ConnectableFlux<OrganisationData> getOrganisationFlux();

    /**
     * Gets a ConnectableFlux which produces with every new user deleted from the
     * administration service. The Flux does not complete.
     *
     * @return the ConnectableFlux of subjects.
     */
    ConnectableFlux<String> getUserDeletionFlux();

    /**
     * Gets a ConnectableFlux which produces with every new organisation deleted from the
     * administration service. The Flux does not complete.
     *
     * @return the ConnectableFlux of organisationIds.
     */
    ConnectableFlux<String> getOrganisationDeletionFlux();
}
