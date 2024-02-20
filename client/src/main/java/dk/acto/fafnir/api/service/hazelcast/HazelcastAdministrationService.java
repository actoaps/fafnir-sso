package dk.acto.fafnir.api.service.hazelcast;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.hazelcast.map.listener.EntryAddedListener;
import com.hazelcast.map.listener.EntryRemovedListener;
import com.hazelcast.map.listener.EntryUpdatedListener;
import dk.acto.fafnir.api.exception.*;
import dk.acto.fafnir.api.model.*;
import dk.acto.fafnir.api.model.conf.HazelcastConf;
import dk.acto.fafnir.api.service.AdministrationService;
import dk.acto.fafnir.api.service.CryptoService;
import dk.acto.fafnir.api.util.CryptoUtil;
import dk.acto.fafnir.client.providers.PublicKeyProvider;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.ConnectableFlux;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.Comparator;
import java.util.Optional;

@AllArgsConstructor
@Service
public class HazelcastAdministrationService implements AdministrationService {
    public static final String USER_POSTFIX = "-fafnir-user";
    public static final String ORG_POSTFIX = "-fafnir-organisation";
    public static final String CLAIM_POSTFIX = "-fafnir-claim";
    private final HazelcastInstance hazelcastInstance;
    private final HazelcastConf hazelcastConf;
    private final PublicKeyProvider publicKeyProvider;
    private final CryptoService cryptoService;

    @Override
    public UserData createUser(final UserData src) {
        var source = hazelcastRules(src);

        IMap<String, UserData> userMap = hazelcastInstance.getMap(hazelcastConf.getPrefix() + USER_POSTFIX);
        if (userMap.containsKey(source.getSubject())) {
            throw new UserAlreadyExists();
        }
        var create = source.toBuilder()
                .created(Instant.now())
                .build();
        var temp = secure(create);
        userMap.put(source.getSubject(), temp);
        return temp;
    }

    @Override
    public UserData readUser(final String source) {
        var subject = hazelcastRules(source);

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
                .map(this::hazelcastRules)
                .orElseThrow(NoSubject::new);
        IMap<String, UserData> userMap = hazelcastInstance.getMap(hazelcastConf.getPrefix() + USER_POSTFIX);
        if (!userMap.containsKey(subject)) {
            throw new NoSuchUser();
        }
        var updated = Optional.ofNullable(userMap.get(subject))
                .map(x -> x.partialUpdate(source))
                .orElseThrow(UserUpdateFailed::new);

        var temp = source.getPassword() == null ? updated : secure(updated);
        userMap.put(subject, temp);
        return temp;
    }

    @Override
    public UserData deleteUser(final String source) {
        var subject = hazelcastRules(source);

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
    public OrganisationData readOrganisation(TenantIdentifier identifier) {
        IMap<String, OrganisationData> orgMap = hazelcastInstance.getMap(hazelcastConf.getPrefix() + ORG_POSTFIX);
        return orgMap.values()
                .stream()
                .filter(entry -> identifier.matches(entry.getProviderConfiguration()))
                .findAny()
                .orElseThrow(NoSuchOrganisation::new);
    }
    @Override
    public Optional<OrganisationData> readOrganisationDoesNotThrow(TenantIdentifier identifier) {
        IMap<String, OrganisationData> orgMap = hazelcastInstance.getMap(hazelcastConf.getPrefix() + ORG_POSTFIX);
        return orgMap.values()
            .stream()
            .filter(entry -> identifier.matches(entry.getProviderConfiguration()))
            .findAny();
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
    public ClaimData createClaim(final OrganisationSubjectPair pairSource, final ClaimData source) {
        var pair = hazelcastRules(pairSource);
        readUser(pair.getSubject());
        readOrganisation(pair.getOrganisationId());
        IMap<OrganisationSubjectPair, ClaimData> claimMap = hazelcastInstance.getMap(hazelcastConf.getPrefix() + CLAIM_POSTFIX);
        if (claimMap.containsKey(pair)) {
            throw new ClaimAlreadyExists();
        }
        claimMap.put(pair, source);
        return source;
    }

    @Override
    public ClaimData readClaims(final OrganisationSubjectPair source) {
        var pair = hazelcastRules(source);
        IMap<OrganisationSubjectPair, ClaimData> claimMap = hazelcastInstance.getMap(hazelcastConf.getPrefix() + CLAIM_POSTFIX);
        if (!claimMap.containsKey(pair)) {
            throw new NoSuchClaim();
        }
        return claimMap.get(pair);
    }

    @Override
    public ClaimData updateClaims(final OrganisationSubjectPair pairSource, final ClaimData source) {
        var pair = hazelcastRules(pairSource);
        IMap<OrganisationSubjectPair, ClaimData> claimMap = hazelcastInstance.getMap(hazelcastConf.getPrefix() + CLAIM_POSTFIX);
        if (!claimMap.containsKey(pair)) {
            throw new NoSuchClaim();
        }
        claimMap.put(pair, source);
        return source;
    }

    @Override
    public ClaimData deleteClaims(final OrganisationSubjectPair source) {
        var pair = hazelcastRules(source);
        IMap<OrganisationSubjectPair, ClaimData> claimMap = hazelcastInstance.getMap(hazelcastConf.getPrefix() + CLAIM_POSTFIX);
        if (!claimMap.containsKey(pair)) {
            throw new NoSuchClaim();
        }
        var result = claimMap.get(pair);
        claimMap.remove(pair);
        return result;
    }

    @Override
    public OrganisationData[] getOrganisationsForUser(String subject) {
        IMap<OrganisationSubjectPair, ClaimData> claimMap = hazelcastInstance.getMap(hazelcastConf.getPrefix() + CLAIM_POSTFIX);
        return claimMap.keySet().stream().filter(x -> x.getSubject().equals(hazelcastRules(subject)))
                .map(OrganisationSubjectPair::getOrganisationId)
                .map(this::readOrganisation)
                .toArray(OrganisationData[]::new);
    }

    @Override
    public UserData[] getUsersForOrganisation(String orgId) {
        IMap<OrganisationSubjectPair, ClaimData> claimMap = hazelcastInstance.getMap(hazelcastConf.getPrefix() + CLAIM_POSTFIX);
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

    @Override
    public ConnectableFlux<UserData> getUserFlux() {
        return getUserFlux(true);
    }

    @Override
    public ConnectableFlux<UserData> getUserFlux(Boolean publishOnUpdate) {
        IMap<String, UserData> userMap = hazelcastInstance.getMap(hazelcastConf.getPrefix() + USER_POSTFIX);

        return Flux.<UserData>create(x -> {
            userMap.addEntryListener((EntryAddedListener<String, UserData>) d -> x.next(d.getValue()), true);
            if (publishOnUpdate) {
                userMap.addEntryListener((EntryUpdatedListener<String, UserData>) d -> x.next(d.getValue()), true);
            }
        }).publish();
    }

    @Override
    public ConnectableFlux<OrganisationData> getOrganisationFlux() {
        return getOrganisationFlux(true);
    }

    @Override
    public ConnectableFlux<OrganisationData> getOrganisationFlux(Boolean publishOnUpdate) {
        IMap<String, OrganisationData> orgMap = hazelcastInstance.getMap(hazelcastConf.getPrefix() + ORG_POSTFIX);

        return Flux.<OrganisationData>create(x -> {
            orgMap.addEntryListener((EntryAddedListener<String, OrganisationData>) d -> x.next(d.getValue()), true);
            if (publishOnUpdate) {
                orgMap.addEntryListener((EntryUpdatedListener<String, OrganisationData>) d -> x.next(d.getValue()), true);
            }
        }).publish();
    }

    @Override
    public ConnectableFlux<String> getUserDeletionFlux() {
        IMap<String, UserData> userMap = hazelcastInstance.getMap(hazelcastConf.getPrefix() + USER_POSTFIX);

        return Flux.<String>create(x -> userMap.addEntryListener(
                (EntryRemovedListener<String, UserData>) d -> x.next(d.getKey()), false
        )).publish();
    }

    @Override
    public ConnectableFlux<String> getOrganisationDeletionFlux() {
        IMap<String, OrganisationData> orgMap = hazelcastInstance.getMap(hazelcastConf.getPrefix() + ORG_POSTFIX);

        return Flux.<String>create(x -> orgMap.addEntryListener(
                (EntryRemovedListener<String, OrganisationData>) d -> x.next(d.getKey()), false
        )).publish();
    }

    private UserData secure(UserData source) {
        var pk = hazelcastConf.isPasswordIsEncrypted() ? CryptoUtil.toPublicKey(publicKeyProvider) : null;

        return source.toBuilder()
                .password(cryptoService.encodePassword(source.getPassword(), pk))
                .build();
    }

    private String hazelcastRules(final String source) {
        var subject = hazelcastConf.isTrimUsername()
                ? source.trim()
                : source;
        return hazelcastConf.isUsernameIsEmail()
                ? subject.toLowerCase()
                : subject;
    }

    private OrganisationSubjectPair hazelcastRules(final OrganisationSubjectPair source) {
        var subject = hazelcastRules(source.getSubject());
        return source.toBuilder()
                .subject(subject)
                .build();
    }

    private UserData hazelcastRules(final UserData source) {
        var subject = hazelcastRules(source.getSubject());
        return source.toBuilder()
                .subject(subject)
                .build();
    }

}
