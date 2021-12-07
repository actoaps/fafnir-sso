package dk.acto.fafnir.api.service;

import com.hazelcast.collection.ISet;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import dk.acto.fafnir.api.exception.*;
import dk.acto.fafnir.api.model.ClaimData;
import dk.acto.fafnir.api.model.FafnirUser;
import dk.acto.fafnir.api.model.OrganisationData;
import dk.acto.fafnir.api.model.UserData;
import dk.acto.fafnir.api.util.CryptoUtil;
import lombok.AllArgsConstructor;
import lombok.Value;

import java.util.Optional;

@Value
@AllArgsConstructor
public class HazelcastAdministrationService implements AdministrationService {
    private final static String USER_POSTFIX = "-fafnir-user";
    private final static String ORG_POSTFIX = "-fafnir-organisation";
    private final static String CLAIM_POSTFIX = "-fafnir-claim";
    HazelcastInstance hazelcastInstance;
    String mapPrefix;

    @Override
    public UserData createUser(UserData source) {
        IMap<String, UserData> userMap = hazelcastInstance.getMap(mapPrefix + USER_POSTFIX);
        if (userMap.containsKey(source.getSubject())) {
            throw new UserAlreadyExists();
        }
        userMap.put(source.getSubject(), source);
        return source;
    }

    @Override
    public UserData readUser(String subject) {
        IMap<String, UserData> userMap = hazelcastInstance.getMap(mapPrefix + USER_POSTFIX);
        return Optional.ofNullable(userMap.get(subject))
                .orElseThrow(NoSuchUser::new);
    }

    @Override
    public UserData updateUser(UserData source) {
        IMap<String, UserData> userMap = hazelcastInstance.getMap(mapPrefix + USER_POSTFIX);
        if (!userMap.containsKey(source.getSubject())) {
            throw new NoSuchUser();
        }
        userMap.put(source.getSubject(), source);
        return source;
    }

    @Override
    public UserData deleteUser(String subject) {
        IMap<String, UserData> userMap = hazelcastInstance.getMap(mapPrefix + USER_POSTFIX);
        if (!userMap.containsKey(subject)) {
            throw new NoSuchUser();
        }
        return userMap.remove(subject);
    }

    @Override
    public OrganisationData createOrganisation(OrganisationData source) {
        IMap<String, OrganisationData> orgMap = hazelcastInstance.getMap(mapPrefix + ORG_POSTFIX);
        if (orgMap.containsKey(source.getOrganisationId())) {
            throw new OrganisationAlreadyExists();
        }
        orgMap.put(source.getOrganisationId(), source);
        return source;
    }

    @Override
    public OrganisationData readOrganisation(String orgId) {
        IMap<String, OrganisationData> orgMap = hazelcastInstance.getMap(mapPrefix + ORG_POSTFIX);
        return Optional.ofNullable(orgMap.get(orgId))
                .orElseThrow(NoSuchOrganisation::new);
    }

    @Override
    public OrganisationData updateOrganisation(OrganisationData source) {
        IMap<String, OrganisationData> orgMap = hazelcastInstance.getMap(mapPrefix + ORG_POSTFIX);
        if (orgMap.containsKey(source.getOrganisationId())) {
            throw new OrganisationAlreadyExists();
        }
        orgMap.put(source.getOrganisationId(), source);
        return source;
    }

    @Override
    public OrganisationData deleteOrganisation(String orgId) {
        IMap<String, OrganisationData> orgMap = hazelcastInstance.getMap(mapPrefix + ORG_POSTFIX);
        if (orgMap.containsKey(orgId)) {
            throw new NoSuchOrganisation();
        }
        return orgMap.remove(orgId);
    }

    @Override
    public ClaimData createClaim(ClaimData source) {
        ISet<ClaimData> claimSet = hazelcastInstance.getSet(mapPrefix + "-claim");
        if (claimSet.contains(source)) {
            throw new ClaimAlreadyExists();
        }
        claimSet.add(source);
        return source;
    }

    @Override
    public ClaimData readClaims(String orgId, String subject) {
        ISet<ClaimData> claimSet = hazelcastInstance.getSet(mapPrefix + "-claim");
        return claimSet.stream().filter(data -> !(data.getSubject().equals(subject) && data.getOrganisationId().equals(orgId)))
                .findAny()
                .orElseThrow(NoSuchClaim::new);
    }

    @Override
    public ClaimData updateClaims(ClaimData source) {
        ISet<ClaimData> claimSet = hazelcastInstance.getSet(mapPrefix + "-claim");
        if (!claimSet.contains(source)) {
            throw new NoSuchClaim();
        }
        claimSet.add(source);
        return source;
    }

    @Override
    public ClaimData deleteClaims(ClaimData source) {
        ISet<ClaimData> claimSet = hazelcastInstance.getSet(mapPrefix + "-claim");
        if (!claimSet.contains(source)) {
            throw new NoSuchClaim();
        }
        claimSet.remove(source);
        return source;
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
