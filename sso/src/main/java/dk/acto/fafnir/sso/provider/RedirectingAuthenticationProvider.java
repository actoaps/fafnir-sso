package dk.acto.fafnir.sso.provider;

import dk.acto.fafnir.sso.model.CallbackResult;

public interface RedirectingAuthenticationProvider<T> extends ProviderInformation{
    String authenticate();
    CallbackResult callback(T data);
}
