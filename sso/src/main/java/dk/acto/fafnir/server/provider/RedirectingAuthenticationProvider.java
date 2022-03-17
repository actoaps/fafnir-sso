package dk.acto.fafnir.server.provider;

import dk.acto.fafnir.server.model.CallbackResult;

public interface RedirectingAuthenticationProvider<T> {
    String authenticate();
    CallbackResult callback(T data);
    boolean supportsOrganisationUrls();
}
