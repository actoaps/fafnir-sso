package dk.acto.fafnir.iam.dto;

import dk.acto.fafnir.api.model.ClaimData;
import dk.acto.fafnir.api.model.OrganisationData;
import dk.acto.fafnir.api.model.Slice;
import dk.acto.fafnir.api.model.UserData;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;

@Service
public class DtoFactory {

    public List<ClaimOrganisationInfo> toInfo(
            List<ClaimData> claims,
            List<OrganisationData> organisations,
            List<UserData> users) {
        return organisations.stream()
                .map(organisation -> ClaimOrganisationInfo.builder()
                        .organisationId(organisation.getOrganisationId())
                        .organisationName(organisation.getOrganisationName())
                        .users(unpackUsers(claims, users, organisation.getOrganisationId())))
                .map(ClaimOrganisationInfo.ClaimOrganisationInfoBuilder::build)
                .collect(Collectors.toList());
    }

    private List<String> unpackClaims(Stream<ClaimData> data, String orgId, String subject) {
        return data.filter(claim -> claim.getOrganisationId().equals(orgId)
                        && claim.getSubject().equals(subject))
                .map(ClaimData::getClaims)
                .findAny()
                .map(Arrays::asList)
                .orElse(List.of());
    }

    private List<ClaimUserInfo> unpackUsers(List<ClaimData> data, List<UserData> users, String orgId) {
        return data.stream().filter(claim -> claim.getOrganisationId().equals(orgId))
                .flatMap(claimData -> users.stream().filter(userData -> userData.getSubject().equals(claimData.getSubject())))
                .map(userData -> ClaimUserInfo.builder()
                        .subject(userData.getSubject())
                        .name(userData.getName())
                        .claims(unpackClaims(data.stream(), orgId, userData.getSubject())))
                .map(ClaimUserInfo.ClaimUserInfoBuilder::build)
                .collect(Collectors.toList());
    }

    public Map<String, Object> calculatePageData(Long pageActual, Long maxValue, String baseUrl) {
        var lastPage = Slice.lastPage(maxValue);
        var result = new TreeMap<String, Object>();
        result.put("nextUrl", pageActual < lastPage ? baseUrl + "/page/" + (pageActual + 2) : null);
        result.put("prevUrl", pageActual > 0 ? baseUrl + "/page/" + (pageActual) : null);
        result.put("pageData",
        LongStream.range(0, lastPage +1)
                .map(x -> x + 1)
                .mapToObj(page -> PageData.builder()
                        .number(BigInteger.valueOf(page))
                        .url(pageActual+1 != page ? baseUrl + "/page/" + (page) : null)
                        .build())
                .toArray());
        return result;
    }
}
