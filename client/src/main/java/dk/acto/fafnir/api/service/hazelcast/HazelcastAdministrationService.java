package dk.acto.fafnir.api.service.hazelcast;

import com.hazelcast.collection.ISet;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import dk.acto.fafnir.api.exception.*;
import dk.acto.fafnir.api.model.*;
import dk.acto.fafnir.api.model.conf.HazelcastConf;
import dk.acto.fafnir.api.service.AdministrationService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.Optional;

@AllArgsConstructor
@Service
public class HazelcastAdministrationService implements AdministrationService {
    public final static String USER_POSTFIX = "-fafnir-user";
    public final static String ORG_POSTFIX = "-fafnir-organisation";
    public final static String CLAIM_POSTFIX = "-fafnir-claim";
    private final HazelcastInstance hazelcastInstance;
    private final HazelcastConf hazelcastConf;

    @Override
    public UserData createUser(final UserData source) {
        IMap<String, UserData> userMap = hazelcastInstance.getMap(hazelcastConf.getPrefix() + USER_POSTFIX);
        if (userMap.containsKey(source.getSubject())) {
            throw new UserAlreadyExists();
        }
        var create = source.toBuilder()
                .created(Instant.now())
                .build();
        userMap.put(source.getSubject(), create);
        return create;
    }

    @Override
    public UserData readUser(String subject) {
        IMap<String, UserData> userMap = hazelcastInstance.getMap(hazelcastConf.getPrefix() + USER_POSTFIX);
        return Optional.ofNullable(userMap.get(subject))
                .orElseThrow(NoSuchUser::new);
    }

    @Override
    public Slice<UserData> readUsers(Long page) {
        IMap<String, UserData> userMap = hazelcastInstance.getMap(hazelcastConf.getPrefix() + USER_POSTFIX);
        var offset = Slice.getOffset(page);
        var total = Long.valueOf(userMap.size());
        return Slice.fromPartial(userMap.values().stream().skip(offset).limit(Slice.PAGE_SIZE), total, x -> x);
    }

    @Override
    public UserData[] readUsers() {
        IMap<String, UserData> userMap = hazelcastInstance.getMap(hazelcastConf.getPrefix() + USER_POSTFIX);
        return userMap.values().toArray(UserData[]::new);
    }

    @Override
    public UserData updateUser(final UserData source) {
        var subject = Optional.ofNullable(source.getSubject())
                .orElseThrow(NoSubject::new);
        IMap<String, UserData> userMap = hazelcastInstance.getMap(hazelcastConf.getPrefix() + USER_POSTFIX);
        if (!userMap.containsKey(subject)) {
            throw new NoSuchUser();
        }
        var updated = Optional.of(userMap.get(subject))
                .map(x -> x.partialUpdate(source))
                .orElseThrow(UserUpdateFailed::new);

        userMap.put(subject, updated);
        return updated;
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
    public OrganisationData createOrganisation(final OrganisationData source) {
        var orgId = source.getOrganisationId();

        IMap<String, OrganisationData> orgMap = hazelcastInstance.getMap(hazelcastConf.getPrefix() + ORG_POSTFIX);
        if (orgMap.containsKey(orgId)) {
            throw new OrganisationAlreadyExists();
        }

        var create = source.toBuilder()
                .created(Instant.now())
                .build();

        orgMap.put(orgId, create);
        return create;
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
        if (!orgMap.containsKey(source.getOrganisationId())) {
            throw new NoSuchOrganisation();
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
        ISet<ClaimData> claimSet = hazelcastInstance.getSet(hazelcastConf.getPrefix() + CLAIM_POSTFIX);
        if (claimSet.contains(source)) {
            throw new ClaimAlreadyExists();
        }
        claimSet.add(source);
        return source;
    }

    @Override
    public Slice<ClaimData> readClaims(Long page) {
        ISet<ClaimData> claimSet = hazelcastInstance.getSet(hazelcastConf.getPrefix() + CLAIM_POSTFIX);
        var offset = Slice.getOffset(page);
        var total = Long.valueOf(claimSet.size());

        return Slice.fromPartial(claimSet.stream().skip(offset).limit(Slice.PAGE_SIZE), total, x -> x);

    }

    @Override
    public ClaimData readClaims(String orgId, String subject) {
        ISet<ClaimData> claimSet = hazelcastInstance.getSet(hazelcastConf.getPrefix() + CLAIM_POSTFIX);
        return claimSet.stream().filter(data -> (data.getSubject().equals(subject) && data.getOrganisationId().equals(orgId)))
                .findAny()
                .orElseThrow(NoSuchClaim::new);
    }

    @Override
    public ClaimData updateClaims(ClaimData source) {
        ISet<ClaimData> claimSet = hazelcastInstance.getSet(hazelcastConf.getPrefix() + CLAIM_POSTFIX);
        if (!claimSet.contains(source)) {
            throw new NoSuchClaim();
        }
        claimSet.add(source);
        return source;
    }

    @Override
    public ClaimData deleteClaims(ClaimData source) {
        ISet<ClaimData> claimSet = hazelcastInstance.getSet(hazelcastConf.getPrefix() + CLAIM_POSTFIX);
        if (!claimSet.contains(source)) {
            throw new NoSuchClaim();
        }
        claimSet.remove(source);
        return source;
    }

    @Override
    public OrganisationData[] getOrganisationsForUser(UserData user) {
        ISet<ClaimData> claimSet = hazelcastInstance.getSet(hazelcastConf.getPrefix() + CLAIM_POSTFIX);
        return claimSet.stream().filter(x -> x.getSubject().equals(user.getSubject()))
                .map(ClaimData::getOrganisationId)
                .map(this::readOrganisation)
                .toArray(OrganisationData[]::new);
    }

    @Override
    public Slice<UserData> getUsersForOrganisation(String orgId, Long page) {
        ISet<ClaimData> claimSet = hazelcastInstance.getSet(hazelcastConf.getPrefix() + CLAIM_POSTFIX);
        var temp = claimSet.stream()
                .filter(x -> x.getOrganisationId().equals(orgId))
                .map(ClaimData::getSubject)
                .sorted().toList();

        var offset = Slice.getOffset(page);
        var total = Long.valueOf(temp.size());

        return Slice.fromPartial(temp.stream().skip(offset), total, this::readUser);
    }

    @Override
    public Slice<OrganisationData> readOrganisations(Long page) {
        IMap<String, OrganisationData> orgMap = hazelcastInstance.getMap(hazelcastConf.getPrefix() + ORG_POSTFIX);
        var offset = Slice.getOffset(page);
        var total = Long.valueOf(orgMap.size());
        return Slice.fromPartial(orgMap.values().stream()
                .sorted(Comparator.comparing(OrganisationData::getOrganisationName))
                .skip(offset).limit(Slice.PAGE_SIZE), total, x -> x);
    }

    @Override
    public OrganisationData[] readOrganisations() {
        IMap<String, OrganisationData> orgMap = hazelcastInstance.getMap(hazelcastConf.getPrefix() + ORG_POSTFIX);
        return orgMap.values().toArray(OrganisationData[]::new);
    }

    @Override
    public Long countOrganisations() {
        IMap<String, OrganisationData> orgMap = hazelcastInstance.getMap(hazelcastConf.getPrefix() + ORG_POSTFIX);
        return (long) orgMap.entrySet().size();
    }
}
