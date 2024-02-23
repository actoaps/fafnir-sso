package dk.acto.fafnir.api.provider;


import com.hazelcast.security.TokenCredentials;
import dk.acto.fafnir.api.model.AuthenticationResult;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

public interface RedirectingAuthenticationProvider<T> extends ProviderInformation {
    String authenticate() throws NoSuchAlgorithmException;

    AuthenticationResult callback(T data) throws IOException;
}
