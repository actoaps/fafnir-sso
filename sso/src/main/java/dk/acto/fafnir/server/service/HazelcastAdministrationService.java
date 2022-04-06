package dk.acto.fafnir.server.service;

import com.hazelcast.collection.ISet;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import dk.acto.fafnir.api.exception.*;
import dk.acto.fafnir.api.model.ClaimData;
import dk.acto.fafnir.api.model.OrganisationData;
import dk.acto.fafnir.api.model.ProviderMetaData;
import dk.acto.fafnir.api.model.UserData;
import dk.acto.fafnir.api.model.conf.HazelcastConf;
import dk.acto.fafnir.api.service.AdministrationService;
import lombok.AllArgsConstructor;
import lombok.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Value
@AllArgsConstructor
@Service
public class HazelcastAdministrationService implements AdministrationService {
    public final static String USER_POSTFIX = "-fafnir-user";
    public final static String ORG_POSTFIX = "-fafnir-organisation";
    public final static String CLAIM_POSTFIX = "-fafnir-claim";
    HazelcastInstance hazelcastInstance;
    HazelcastConf hazelcastConf;

    @Override
    public UserData createUser(UserData source) {
        IMap<String, UserData> userMap = hazelcastInstance.getMap(hazelcastConf.getPrefix() + USER_POSTFIX);
        if (userMap.containsKey(source.getSubject())) {
            throw new UserAlreadyExists();
        }
        userMap.put(source.getSubject(), source);
        return source;
    }

    @Override
    public UserData readUser(String subject) {
        IMap<String, UserData> userMap = hazelcastInstance.getMap(hazelcastConf.getPrefix() + USER_POSTFIX);
        return Optional.ofNullable(userMap.get(subject))
                .orElseThrow(NoSuchUser::new);
    }

    @Override
    public UserData[] readUsers() {
        IMap<String, UserData> userMap = hazelcastInstance.getMap(hazelcastConf.getPrefix() + USER_POSTFIX);
        return userMap.values().toArray(UserData[]::new);
    }

    @Override
    public UserData updateUser(UserData source) {
        IMap<String, UserData> userMap = hazelcastInstance.getMap(hazelcastConf.getPrefix() + USER_POSTFIX);
        if (!userMap.containsKey(source.getSubject())) {
            throw new NoSuchUser();
        }
        userMap.put(source.getSubject(), source);
        return source;
    }

    @Override
    public UserData deleteUser(String subject) {
        IMap<String, UserData> userMap = hazelcastInstance.getMap(hazelcastConf.getPrefix() + USER_POSTFIX);
        if (!userMap.containsKey(subject)) {
            throw new NoSuchUser();
        }
        return userMap.remove(subject);
    }

    @Override
    public OrganisationData createOrganisation(OrganisationData source) {
        IMap<String, OrganisationData> orgMap = hazelcastInstance.getMap(hazelcastConf.getPrefix() + ORG_POSTFIX);
        if (orgMap.containsKey(source.getOrganisationId())) {
            throw new OrganisationAlreadyExists();
        }
        orgMap.put(source.getOrganisationId(), source);
        return source;
    }

    @Override
    public OrganisationData readOrganisation(String orgId) {
        IMap<String, OrganisationData> orgMap = hazelcastInstance.getMap(hazelcastConf.getPrefix() + ORG_POSTFIX);
        return Optional.ofNullable(orgMap.get(orgId))
                .orElseThrow(NoSuchOrganisation::new);
    }

    @Override
    public OrganisationData readOrganisation(String providerKey, String providerValue) {
        IMap<String, OrganisationData> orgMap = hazelcastInstance.getMap(hazelcastConf.getPrefix() + ORG_POSTFIX);
        return orgMap.values().stream()
                .filter(x -> x.getProviderConfigurations().stream()
                        .filter(y -> y.hasValue(providerKey, providerValue)).findAny().isEmpty())
                .findAny()
                .orElseThrow(NoSuchOrganisation::new);
    }

    @Override
    public OrganisationData readOrganisation(ProviderMetaData providerMetaData) {
        IMap<String, OrganisationData> orgMap = hazelcastInstance.getMap(hazelcastConf.getPrefix() + ORG_POSTFIX);
        return orgMap.values().stream()
                .filter(x -> x.getProviderConfigurations().stream()
                        .filter(y -> providerMetaData.getProviderId().equals(y.getProviderId())).findFirst().isEmpty())
                .findAny()
                .orElseThrow(NoSuchOrganisation::new);
    }

    @Override
    public OrganisationData updateOrganisation(OrganisationData source) {
        IMap<String, OrganisationData> orgMap = hazelcastInstance.getMap(hazelcastConf.getPrefix() + ORG_POSTFIX);
        if (orgMap.containsKey(source.getOrganisationId())) {
            throw new OrganisationAlreadyExists();
        }
        orgMap.put(source.getOrganisationId(), source);
        return source;
    }

    @Override
    public OrganisationData deleteOrganisation(String orgId) {
        IMap<String, OrganisationData> orgMap = hazelcastInstance.getMap(hazelcastConf.getPrefix() + ORG_POSTFIX);
        if (orgMap.containsKey(orgId)) {
            throw new NoSuchOrganisation();
        }
        return orgMap.remove(orgId);
    }

    @Override
    public ClaimData createClaim(ClaimData source) {
        ISet<ClaimData> claimSet = hazelcastInstance.getSet(hazelcastConf.getPrefix() + "-claim");
        if (claimSet.contains(source)) {
            throw new ClaimAlreadyExists();
        }
        claimSet.add(source);
        return source;
    }

    @Override
    public ClaimData readClaims(String orgId, String subject) {
        ISet<ClaimData> claimSet = hazelcastInstance.getSet(hazelcastConf.getPrefix() + "-claim");
        return claimSet.stream().filter(data -> (data.getSubject().equals(subject) && data.getOrganisationId().equals(orgId)))
                .findAny()
                .orElseThrow(NoSuchClaim::new);
    }

    @Override
    public ClaimData updateClaims(ClaimData source) {
        ISet<ClaimData> claimSet = hazelcastInstance.getSet(hazelcastConf.getPrefix() + "-claim");
        if (!claimSet.contains(source)) {
            throw new NoSuchClaim();
        }
        claimSet.add(source);
        return source;
    }

    @Override
    public ClaimData deleteClaims(ClaimData source) {
        ISet<ClaimData> claimSet = hazelcastInstance.getSet(hazelcastConf.getPrefix() + "-claim");
        if (!claimSet.contains(source)) {
            throw new NoSuchClaim();
        }
        claimSet.remove(source);
        return source;
    }

    @Override
    public OrganisationData[] getOrganisationsForUser(UserData user) {
        ISet<ClaimData> claimSet = hazelcastInstance.getSet(hazelcastConf.getPrefix() + "-claim");
        return claimSet.stream().filter(x -> x.getSubject().equals(user.getSubject()))
                .map(ClaimData::getOrganisationId)
                .map(this::readOrganisation)
                .toArray(OrganisationData[]::new);
    }

    @Override
    public UserData[] getUsersForOrganisation(String orgId) {
        ISet<ClaimData> claimSet = hazelcastInstance.getSet(hazelcastConf.getPrefix() + "-claim");
        return claimSet.stream().filter(x -> x.getOrganisationId().equals(orgId))
                .map(ClaimData::getSubject)
                .map(this::readUser)
                .toArray(UserData[]::new);
    }

    @Override
    public OrganisationData[] readOrganisations() {
        IMap<String, OrganisationData> orgMap = hazelcastInstance.getMap(hazelcastConf.getPrefix() + ORG_POSTFIX);
        return orgMap.values().toArray(OrganisationData[]::new);
    }
}
