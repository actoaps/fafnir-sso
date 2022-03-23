package dk.acto.fafnir.server.provider;

import dk.acto.fafnir.server.model.CallbackResult;

public interface RedirectingAuthenticationProvider<T> extends ProviderInformation{
    String authenticate();
    CallbackResult callback(T data);
}
