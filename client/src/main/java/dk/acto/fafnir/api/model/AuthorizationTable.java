package dk.acto.fafnir.api.model;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import dk.acto.fafnir.api.exception.NoSuchClaim;
import dk.acto.fafnir.api.exception.NoSuchOrganisation;
import dk.acto.fafnir.api.exception.NoSuchUser;

import java.util.*;

public final class AuthorizationTable {
    private final Map<String, UserData> userLookup = new HashMap<>();
    private final Map<String, OrganisationData> organisationLookup = new HashMap<>();
    private final Set<String> validClaims = new HashSet<>();
    private final Table<UserData, OrganisationData, Set<String>> claimsTable = HashBasedTable.create();

    public synchronized UserData createUser (UserData source) {
        userLookup.put(source.getSubject(), source);
        return source;
    }

    public synchronized OrganisationData createOrganisationData (OrganisationData source) {
        organisationLookup.put(source.getOrganisationId(), source);
        return source;
    }

    public synchronized String  createClaim (String source) {
        validClaims.add(source);
        return source;
    }

    public synchronized void assignClaim(String subject, String orgId, String claim) {
        var user = Optional.ofNullable(userLookup.get(subject))
                        .orElseThrow(NoSuchUser::new);
        var organisation = Optional.ofNullable(organisationLookup.get(orgId))
                        .orElseThrow(NoSuchOrganisation::new);
        if (!validClaims.contains(claim)) {
            throw new NoSuchClaim();
        }
        var existingClaims = Optional.ofNullable(claimsTable.get(user, organisation))
                        .orElse(new HashSet<>());
        existingClaims.add(claim);
        claimsTable.put(user, organisation, existingClaims);
    }
}
