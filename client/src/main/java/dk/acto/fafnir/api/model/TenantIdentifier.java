package dk.acto.fafnir.api.model;

public interface TenantIdentifier {
    boolean matches(ProviderConfiguration test);
}
