package dk.acto.fafnir.providers;

import dk.acto.fafnir.model.CallbackResult;

public interface RedirectingAuthenticationProvider<T> {
    String authenticate();
    CallbackResult callback(T data);
}
