package dk.acto.fafnir.api.service;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import dk.acto.fafnir.api.exception.NoSuchUser;
import dk.acto.fafnir.api.exception.UserAlreadyExists;
import dk.acto.fafnir.api.model.ClaimData;
import dk.acto.fafnir.api.model.FafnirUser;
import dk.acto.fafnir.api.model.OrganisationData;
import dk.acto.fafnir.api.model.UserData;
import lombok.AllArgsConstructor;
import lombok.Value;

import java.util.Optional;

@Value
@AllArgsConstructor
public class HazelcastAuthorisationService implements AuthorisationService{
    HazelcastInstance hazelcastInstance;
    String mapPrefix;

    @Override
    public UserData createUser(UserData source) {
        IMap<String, UserData> userMap = hazelcastInstance.getMap(mapPrefix + "-user");
        if (userMap.containsKey(source.getSubject())) {
            throw new UserAlreadyExists();
        }
        userMap.put(source.getSubject(), source);
        return source;
    }

    @Override
    public UserData readUser(String subject) {
        IMap<String, UserData> userMap = hazelcastInstance.getMap(mapPrefix + "-user");
        return Optional.ofNullable(userMap.get(subject))
                .orElseThrow(NoSuchUser::new);
    }

    @Override
    public UserData updateUser(UserData source) {
        IMap<String, UserData> userMap = hazelcastInstance.getMap(mapPrefix + "-user");
        if (!userMap.containsKey(source.getSubject())) {
            throw new NoSuchUser();
        }
        userMap.put(source.getSubject(), source);
        return source;
    }

    @Override
    public UserData deleteUser(String subject) {
        return null;
    }

    @Override
    public OrganisationData createOrganisation(OrganisationData source) {
        return null;
    }

    @Override
    public OrganisationData readOrganisation(String orgId) {
        return null;
    }

    @Override
    public OrganisationData updateOrganisation(OrganisationData source) {
        return null;
    }

    @Override
    public OrganisationData deleteOrganisation(String source) {
        return null;
    }

    @Override
    public ClaimData createClaim(ClaimData source) {
        return null;
    }

    @Override
    public ClaimData readClaims(String orgId, String subject) {
        return null;
    }

    @Override
    public ClaimData updateClaims(ClaimData source) {
        return null;
    }

    @Override
    public ClaimData deleteClaims(ClaimData source) {
        return null;
    }

    @Override
    public UserData authenticate(String userName, String password) {
        return null;
    }

    @Override
    public FafnirUser authorize(UserData user, String orgId) {
        return null;
    }

    @Override
    public OrganisationData[] getOrganisationsForUser(UserData user) {
        return new OrganisationData[0];
    }

    @Override
    public UserData[] getUsersForOrganisation(String orgId) {
        return new UserData[0];
    }

    @Override
    public OrganisationData[] getOrganisations() {
        return new OrganisationData[0];
    }
}
