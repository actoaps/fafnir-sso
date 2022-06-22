package dk.acto.fafnir.iam.dto;

import dk.acto.fafnir.api.model.ClaimData;
import dk.acto.fafnir.api.model.OrganisationData;
import dk.acto.fafnir.api.model.Slice;
import dk.acto.fafnir.api.model.UserData;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;
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

    public ClaimInfo toInfo(UserData userData, ClaimData claims) {
        return ClaimInfo.builder()
                .name(userData.getName())
                .id(userData.getSubject())
                .csvClaims(String.join(", ", claims.getClaims()))
                .build();
    }

    public ClaimInfo toInfo(OrganisationData organisationData, ClaimData claims) {
        return ClaimInfo.builder()
                .name(organisationData.getOrganisationName())
                .id(organisationData.getOrganisationId())
                .csvClaims(String.join(", ", claims.getClaims()))
                .build();
    }
}
