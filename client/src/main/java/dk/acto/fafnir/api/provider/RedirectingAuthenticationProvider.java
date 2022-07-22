package dk.acto.fafnir.api.provider;


import dk.acto.fafnir.api.model.AuthenticationResult;

public interface RedirectingAuthenticationProvider<T> extends ProviderInformation{
    String authenticate();
    AuthenticationResult callback(T data);
}
