package dk.acto.fafnir.iam.dto;

import dk.acto.fafnir.api.model.*;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

@Service
public class DtoFactory {

    public Map<String, Object> calculatePageData(Long pageActual, Long maxValue, String baseUrl) {
        var lastPage = Slice.lastPage(maxValue);
        var result = new TreeMap<String, Object>();
        result.put("nextUrl", pageActual < lastPage ? baseUrl + "/page/" + (pageActual + 2) : null);
        result.put("prevUrl", pageActual > 0 ? baseUrl + "/page/" + (pageActual) : null);
        result.put("pageData",
        LongStream.range(1, lastPage +1)
                .mapToObj(page -> PageData.builder()
                        .number(BigInteger.valueOf(page))
                        .url(pageActual+1 != page ? baseUrl + "/page/" + (page) : null)
                        .build())
                .toArray());
        return result;
    }

    public ClaimInfo toInfo(String orgId, UserData userData, ClaimData claims) {
        return ClaimInfo.builder()
                .name(userData.getName())
                .id(userData.getSubject())
                .csvClaims(String.join(", ", claims.getClaims()))
                .url(String.format("/iam/clm/for/%s/%s", orgId, userData.getSubject()))
                .build();
    }

    public ClaimInfo toInfo(String subject, OrganisationData organisationData, ClaimData claims) {
        return ClaimInfo.builder()
                .name(organisationData.getOrganisationName())
                .id(organisationData.getOrganisationId())
                .csvClaims(String.join(", ", claims.getClaims()))
                .url(String.format("/iam/clm/for/%s/%s", organisationData.getOrganisationId(), subject))
                .build();
    }

    public ProviderConfiguration fromMap (Map<String, String> source) {
        return ProviderConfiguration.builder()
                .providerId(source.get("providerId"))
                .values(source.entrySet().stream()
                        .filter(x -> !x.getKey().equals("providerId"))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
                )
                .build();
    }
}
