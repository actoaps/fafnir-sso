package dk.acto.fafnir.api.provider.metadata;

import dk.acto.fafnir.api.model.OrganisationSupport;
import dk.acto.fafnir.api.model.ProviderConfiguration;
import dk.acto.fafnir.api.model.ProviderMetaData;
import io.vavr.control.Try;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class MetadataProvider {

    // From: https://docs.microsoft.com/en-us/azure/active-directory/develop/id-tokens
    private static final String PERSONAL_TENANT_GUID = "9188040d-6c67-4c5b-b112-36a304b66dad";

    public static final ProviderMetaData APPLE = ProviderMetaData.builder()
            .inputs(List.of())
            .organisationSupport(OrganisationSupport.SINGLE)
            .providerName("Apple")
            .providerId("apple")
            .build();
    public static final ProviderMetaData ECONOMIC = ProviderMetaData.builder()
            .providerId("economic")
            .providerName("Economic Customer")
            .inputs(List.of())
            .organisationSupport(OrganisationSupport.SINGLE)
            .build();

    public static final ProviderMetaData FACEBOOK = ProviderMetaData.builder()
            .providerId("facebook")
            .providerName("Facebook")
            .inputs(List.of())
            .organisationSupport(OrganisationSupport.SINGLE)
            .build();

    public static final ProviderMetaData GOOGLE = ProviderMetaData.builder()
            .providerName("Google")
            .providerId("google")
            .organisationSupport(OrganisationSupport.NATIVE)
            .inputs(List.of("Organisation Domain", "Catchall Organisation"))
            .build();

    public static final ProviderMetaData HAZELCAST = ProviderMetaData.builder()
            .providerName("Hazelcast (Built-In)")
            .providerId("hazelcast")
            .organisationSupport(OrganisationSupport.FAFNIR)
            .inputs(List.of())
            .build();

    public static final ProviderMetaData LINKEDIN = ProviderMetaData.builder()
            .providerId("linkedin")
            .providerName("LinkedIn")
            .organisationSupport(OrganisationSupport.SINGLE)
            .inputs(List.of())
            .build();

    public static final ProviderMetaData MS_IDENTITY = ProviderMetaData.builder()
            .providerName(String.format("Microsoft (Personal TenantId is : %s)", PERSONAL_TENANT_GUID))
            .providerId("msidentity")
            .organisationSupport(OrganisationSupport.NATIVE)
            .inputs(List.of("TenantId", "Catchall Organisation"))
            .build();

    public static final ProviderMetaData MIT_ID = ProviderMetaData.builder()
            .inputs(List.of())
            .organisationSupport(OrganisationSupport.SINGLE)
            .providerName("MitID")
            .providerId("mitid")
            .build();

    public static final ProviderMetaData SAML = ProviderMetaData.builder()
            .providerId("saml")
            .providerName("SAML")
            .inputs(List.of("Metadata Location", "Registration Id"))
            .organisationSupport(OrganisationSupport.MULTIPLE)
            .build();

    public static final ProviderMetaData TEST = ProviderMetaData.builder()
            .providerId("test")
            .providerName("Test Provider (Do not use in production)")
            .inputs(List.of())
            .organisationSupport(OrganisationSupport.FAFNIR)
            .build();

    public static ProviderMetaData[] getAllSupportedProviders() {
        return Arrays.stream(MetadataProvider.class.getFields())
                .map(field -> Try.of(() -> field.get(null)).getOrNull())
                .filter(Objects::nonNull)
                .filter(ProviderMetaData.class::isInstance)
                .map(ProviderMetaData.class::cast)
                .toArray(ProviderMetaData[]::new);
    }

    public static ProviderConfiguration empty(ProviderMetaData metaData) {
        return ProviderConfiguration.builder()
                .providerId(metaData.getProviderId())
                .values(metaData.getInputs().stream()
                        .collect(Collectors.toMap(key -> key, value -> "")))
                .build();
    }
}
