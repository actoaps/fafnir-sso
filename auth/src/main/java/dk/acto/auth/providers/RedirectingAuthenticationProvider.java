package dk.acto.auth.providers;

public interface RedirectingAuthenticationProvider<T> {
    String authenticate();
    String callback(T data);
}
