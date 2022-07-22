package dk.acto.fafnir.api.service.hazelcast;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import dk.acto.fafnir.api.crypto.RsaKeyManager;
import dk.acto.fafnir.api.exception.*;
import dk.acto.fafnir.api.model.*;
import dk.acto.fafnir.api.model.conf.HazelcastConf;
import dk.acto.fafnir.api.service.AdministrationService;
import dk.acto.fafnir.api.util.CryptoUtil;
import dk.acto.fafnir.client.providers.PublicKeyProvider;
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
    private PublicKeyProvider publicKeyProvider;

    @Override
    public UserData createUser(final UserData source) {
        IMap<String, UserData> userMap = hazelcastInstance.getMap(hazelcastConf.getPrefix() + USER_POSTFIX);
        if (userMap.containsKey(source.getSubject())) {
            throw new UserAlreadyExists();
        }
        var create = source.toBuilder()
                .created(Instant.now())
                .build();
        userMap.put(source.getSubject(), secure(create));
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

        userMap.put(subject,source.getPassword() == null ? updated : secure(updated));
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
    public OrganisationData readOrganisation(TennantIdentifier identifier) {
        IMap<String, OrganisationData> orgMap = hazelcastInstance.getMap(hazelcastConf.getPrefix() + ORG_POSTFIX);
        return orgMap.values(entry -> identifier.matches(entry.getValue().getProviderConfiguration()))
                .stream().findAny()
                .orElseThrow(NoSuchOrganisation::new);
    }

    @Override
    public OrganisationData updateOrganisation(OrganisationData source) {
        var orgId = Optional.ofNullable(source.getOrganisationId())
                .orElseThrow(NoOrganisationId::new);
        IMap<String, OrganisationData> orgMap = hazelcastInstance.getMap(hazelcastConf.getPrefix() + ORG_POSTFIX);
        if (!orgMap.containsKey(orgId)) {
            throw new NoSuchOrganisation();
        }
        var updated = Optional.of(orgMap.get(orgId))
                .map(x -> x.partialUpdate(source))
                .orElseThrow(NoOrganisationId::new);
        orgMap.put(orgId, updated);
        return updated;
    }

    @Override
    public OrganisationData deleteOrganisation(String orgId) {
        IMap<String, OrganisationData> orgMap = hazelcastInstance.getMap(hazelcastConf.getPrefix() + ORG_POSTFIX);
        if (!orgMap.containsKey(orgId)) {
            throw new NoSuchOrganisation();
        }
        return orgMap.remove(orgId);
    }

    @Override
    public ClaimData createClaim(final OrganisationSubjectPair pair, final ClaimData source) {
        readUser(pair.getSubject());
        readOrganisation(pair.getOrganisationId());
        IMap<OrganisationSubjectPair,ClaimData> claimMap = hazelcastInstance.getMap(hazelcastConf.getPrefix() + CLAIM_POSTFIX);
        if (claimMap.containsKey(pair)) {
            throw new ClaimAlreadyExists();
        }
        claimMap.put(pair, source);
        return source;
    }

    @Override
    public ClaimData readClaims(final OrganisationSubjectPair pair) {
        IMap<OrganisationSubjectPair,ClaimData> claimMap = hazelcastInstance.getMap(hazelcastConf.getPrefix() + CLAIM_POSTFIX);
        if (!claimMap.containsKey(pair)){
            throw new NoSuchClaim();
        }
        return claimMap.get(pair);
    }

    @Override
    public ClaimData updateClaims(final OrganisationSubjectPair pair, final ClaimData source) {
        IMap<OrganisationSubjectPair,ClaimData> claimMap = hazelcastInstance.getMap(hazelcastConf.getPrefix() + CLAIM_POSTFIX);
        if (!claimMap.containsKey(pair)) {
            throw new NoSuchClaim();
        }
        claimMap.put(pair, source);
        return source;
    }

    @Override
    public ClaimData deleteClaims(final OrganisationSubjectPair pair) {
        IMap<OrganisationSubjectPair,ClaimData> claimMap = hazelcastInstance.getMap(hazelcastConf.getPrefix() + CLAIM_POSTFIX);
        if (!claimMap.containsKey(pair)) {
            throw new NoSuchClaim();
        }
        var result = claimMap.get(pair);
        claimMap.remove(pair);
        return result;
    }

    @Override
    public OrganisationData[] getOrganisationsForUser(String subject) {
        IMap<OrganisationSubjectPair,ClaimData> claimMap = hazelcastInstance.getMap(hazelcastConf.getPrefix() + CLAIM_POSTFIX);
        return claimMap.keySet().stream().filter(x -> x.getSubject().equals(subject))
                .map(OrganisationSubjectPair::getOrganisationId)
                .map(this::readOrganisation)
                .toArray(OrganisationData[]::new);
    }

    @Override
    public UserData[] getUsersForOrganisation(String orgId) {
        IMap<OrganisationSubjectPair,ClaimData> claimMap = hazelcastInstance.getMap(hazelcastConf.getPrefix() + CLAIM_POSTFIX);
        return claimMap.keySet().stream()
                .filter(x -> x.getOrganisationId().equals(orgId))
                .map(OrganisationSubjectPair::getSubject)
                .sorted()
                .map(this::readUser)
                .toArray(UserData[]::new);
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

    private UserData secure (UserData source) {
        return source.secure(hazelcastConf.isPasswordIsEncrypted()
                ? CryptoUtil.toPublicKey(publicKeyProvider)
                : null);
    }
}
