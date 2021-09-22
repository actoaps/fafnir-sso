package dk.acto.fafnir.api.model;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import dk.acto.fafnir.api.ClaimConsumer;
import dk.acto.fafnir.api.exception.PasswordMismatch;
import lombok.Synchronized;

import java.util.*;
import java.util.stream.Stream;

public final class AuthorizationTable implements ClaimConsumer{
    private final Map<String, UserData> userLookup = new HashMap<>();
    private final Map<String, OrganisationData> organisationLookup = new HashMap<>();
    // Rows are users, columns are organisations
    private final Table<String, String, Set<String>> claimsTable = HashBasedTable.create();

    @Override
    @Synchronized
    public ClaimsPayload consume(ClaimsPayload payload) {
        var user = payload.getUser();
        userLookup.put(user.getSubject(), user);
        var org = payload.getOrganisation();
        organisationLookup.put(org.getOrganisationId(), org);
        var claim = Optional.ofNullable(claimsTable.get(user.getSubject(),org.getOrganisationId()))
                .orElse(new HashSet<>());
        claim.add(payload.getClaim());
        claimsTable.put(user.getSubject(), org.getOrganisationId(), claim);
        return payload;
    }

    @Override
    public ClaimsPayload destroy(ClaimsPayload payload) {
        var user = payload.getUser();
        var org = payload.getOrganisation();
        var claim = Optional.ofNullable(claimsTable.get(user.getSubject(),org.getOrganisationId()))
                .orElse(new HashSet<>());
        claim.remove(payload.getClaim());
        if (claim.isEmpty()) {
            claimsTable.remove(user.getSubject(), org.getOrganisationId());
        } else {
            claimsTable.put(user.getSubject(), org.getOrganisationId(), claim);
        }
        return payload;
    }

    public Stream<OrganisationData> getOrganisations() {
        return claimsTable.columnKeySet()
                .stream()
                .map(organisationLookup::get);
    }

    public Stream<UserData> getUsersForOrganisation(String orgId) {
        return claimsTable.columnMap().get(orgId).keySet()
                .stream()
                .map(userLookup::get);
    }

    public Stream<String> getClaimsforOrganisationAndUser(String orgId, String subject) {
        return Optional.ofNullable(claimsTable.get(subject, orgId))
                .orElse(new HashSet<>())
                .stream();
    }

    public FafnirUser getUser (String orgId, String subject, String password  ) {
        var user = userLookup.get(subject);
        var org = organisationLookup.get(orgId);
        var claim = Optional.ofNullable(claimsTable.get(user.getSubject(),org.getOrganisationId()))
                .orElse(new HashSet<>());
        if (!password.equals(user.getPassword())) {
            throw new PasswordMismatch();
        }
        return toInfo(user, org, claim);
    }

   public Stream<ClaimsPayload> dump() {
        return claimsTable.cellSet()
                .stream()
                .filter(x -> x.getValue() != null)
                .filter(x -> !x.getValue().isEmpty())
                .flatMap(x -> x.getValue().stream()
                        .map(y -> ClaimsPayload.builder()
                                .claim(y)
                                .organisation(organisationLookup.get(x.getColumnKey()))
                                .user(userLookup.get(x.getRowKey()))
                                .build())
                );
    }

    private FafnirUser toInfo(UserData userData, OrganisationData organisationData, Set<String> claims) {
        return FafnirUser.builder()
                .roles(claims.toArray(String[]::new))
                .data(userData)
                .organisationId(organisationData.getOrganisationId())
                .organisationName(organisationData.getOrganisationName())
                .build();
    }
}
