package dk.acto.fafnir.iam.service.controller;

import dk.acto.fafnir.api.model.ClaimData;
import dk.acto.fafnir.api.model.OrganisationSubjectPair;
import dk.acto.fafnir.api.model.Slice;
import dk.acto.fafnir.api.model.UserData;
import dk.acto.fafnir.api.service.AdministrationService;
import dk.acto.fafnir.iam.dto.ClaimOrganisationInfo;
import dk.acto.fafnir.iam.dto.DtoFactory;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@Slf4j
@AllArgsConstructor
@RequestMapping("iam/clm")
public class ClaimsController
{
    private final AdministrationService administrationService;
    private final DtoFactory dtoFactory;
    @GetMapping("org/{orgId}")
    public ModelAndView getClaimsOverview(@PathVariable String orgId) {
        var users = administrationService.getUsersForOrganisation(orgId);
        var userClaims = Arrays.stream(users)
                .map(userData -> dtoFactory.toInfo(userData, administrationService.readClaims(OrganisationSubjectPair.builder()
                        .subject(userData.getSubject())
                        .organisationId(orgId)
                        .build())))
                .toList();
        var org = administrationService.readOrganisation(orgId);
        var transformed = ClaimOrganisationInfo.builder()
                .organisationName(org.getOrganisationName())
                .organisationId(org.getOrganisationId())
                .users(userClaims)
                .build();
        var model = Map.of(
                "tableData", transformed
        );
        return new ModelAndView("claims_overview", model);
    }


    @GetMapping()
    public ModelAndView createClaims() {
        var users = administrationService.readUsers();
        var organisations = administrationService.readOrganisations();
        var model = Map.of("users", users,
                "organisations", organisations,
                "action", "Create ",
                "verb", "post"
        );
        return new ModelAndView("claims_detail", model);
    }
    @PostMapping()
    public RedirectView addClaims(
            @RequestParam final String organisationId,
            @RequestParam final String subject,
            @RequestParam final String[] claims
            ) {
        administrationService.createClaim(OrganisationSubjectPair.builder()
                        .organisationId(organisationId)
                        .subject(subject)
                .build(), ClaimData.builder()
                        .claims(claims)
                .build());
        return new RedirectView("/iam/clm/page/1");
    }

    @GetMapping("{orgId}/{subject}")
    public ModelAndView getClaims(@PathVariable String orgId, @PathVariable String subject) {
        var result = administrationService.readClaims(OrganisationSubjectPair.builder()
                        .organisationId(orgId)
                        .subject(subject)
                .build());
        var model = Map.of("tableData", result);
        return new ModelAndView("claims_detail", model);
    }
}
