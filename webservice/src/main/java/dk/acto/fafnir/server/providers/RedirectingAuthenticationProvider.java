package dk.acto.fafnir.server.providers;

import dk.acto.fafnir.server.model.CallbackResult;

public interface RedirectingAuthenticationProvider<T> {
    String authenticate();
    CallbackResult callback(T data);
}
