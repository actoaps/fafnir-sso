package dk.acto.fafnir.api.model;

public interface TennantIdentifier {
    boolean matches(ProviderConfiguration test);
}
