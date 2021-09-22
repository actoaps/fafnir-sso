package dk.acto.fafnir.api;

import dk.acto.fafnir.api.model.AuthorizationTable;

public interface TablePersister {
    AuthorizationTable persist(AuthorizationTable authorizationTable);
}
