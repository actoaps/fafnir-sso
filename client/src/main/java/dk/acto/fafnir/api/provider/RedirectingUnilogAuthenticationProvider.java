package dk.acto.fafnir.api.provider;

import dk.acto.fafnir.api.model.AuthenticationResult;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;


public interface RedirectingUnilogAuthenticationProvider<T> extends ProviderInformation {

    String authenticate(HttpSession session) throws NoSuchAlgorithmException;

    AuthenticationResult callback(T data, HttpSession session) throws IOException;
}
