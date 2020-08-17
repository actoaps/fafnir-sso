package dk.acto.auth.providers;

import dk.acto.auth.model.CallbackResult;

public interface RedirectingAuthenticationProvider<T> {
    String authenticate();
    CallbackResult callback(T data);
}
