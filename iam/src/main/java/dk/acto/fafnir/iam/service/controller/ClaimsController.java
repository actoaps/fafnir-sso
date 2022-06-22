package dk.acto.fafnir.iam.service.controller;

import dk.acto.fafnir.api.model.ClaimData;
import dk.acto.fafnir.api.model.OrganisationSubjectPair;
import dk.acto.fafnir.api.service.AdministrationService;
import dk.acto.fafnir.iam.dto.DtoFactory;
import io.vavr.control.Try;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import java.util.Arrays;
import java.util.Map;

@Controller
@Slf4j
@AllArgsConstructor
@RequestMapping("iam/clm")
public class ClaimsController {
    private final AdministrationService administrationService;
    private final DtoFactory dtoFactory;

    @GetMapping("org/{orgId}")
    public ModelAndView getClaimsOverviewForUser(@PathVariable final String orgId) {
        var users = administrationService.getUsersForOrganisation(orgId);
        var transformed = Arrays.stream(users)
                .map(userData -> dtoFactory.toInfo(userData, administrationService.readClaims(OrganisationSubjectPair.builder()
                        .subject(userData.getSubject())
                        .organisationId(orgId)
                        .build())))
                .toList();
        var model = Map.of(
                "tableData", transformed,
                "isUser", true
        );
        return new ModelAndView("claims_overview", model);
    }

    @GetMapping("usr/{subject}")
    public ModelAndView getClaimsOverview(@PathVariable final String subject) {
        var orgs = administrationService.getOrganisationsForUser(subject);
        var transformed = Arrays.stream(orgs)
                .map(organisationData -> dtoFactory.toInfo(organisationData, administrationService.readClaims(OrganisationSubjectPair.builder()
                        .subject(subject)
                        .organisationId(organisationData.getOrganisationId())
                        .build())))
                .toList();
        var model = Map.of(
                "tableData", transformed,
                "isUser", false
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
        return new RedirectView("/iam/clm/org/" + organisationId);
    }
}
