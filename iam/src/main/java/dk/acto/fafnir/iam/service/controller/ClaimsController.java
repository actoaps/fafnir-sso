package dk.acto.fafnir.iam.service.controller;

import dk.acto.fafnir.api.model.ClaimData;
import dk.acto.fafnir.api.model.OrganisationData;
import dk.acto.fafnir.api.model.OrganisationSubjectPair;
import dk.acto.fafnir.api.service.AdministrationService;
import dk.acto.fafnir.api.service.ProviderService;
import dk.acto.fafnir.iam.dto.DtoFactory;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Controller
@Slf4j
@AllArgsConstructor
@RequestMapping("iam/clm")
@PreAuthorize("isAuthenticated()")
public class ClaimsController {
    private final ProviderService providerService;
    private final AdministrationService administrationService;
    private final DtoFactory dtoFactory;

    @GetMapping("org/{orgId}")
    public ModelAndView getClaimsOverviewForUser(@PathVariable final String orgId) {
        var users = administrationService.getUsersForOrganisation(orgId);
        var transformed = Arrays.stream(users)
                .map(userData -> dtoFactory.toInfo(
                        orgId,
                        userData, administrationService.readClaims(OrganisationSubjectPair.builder()
                        .subject(userData.getSubject())
                        .organisationId(orgId)
                        .build())))
                .collect(Collectors.toList());
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
                .map(organisationData -> dtoFactory.toInfo(
                        subject,
                        organisationData, administrationService.readClaims(OrganisationSubjectPair.builder()
                        .subject(subject)
                        .organisationId(organisationData.getOrganisationId())
                        .build())))
                .collect(Collectors.toList());
        var model = Map.of(
                "tableData", transformed,
                "isUser", false
        );
        return new ModelAndView("claims_overview", model);
    }


    @GetMapping()
    public ModelAndView createClaims() {
        var users = administrationService.readUsers();
        var organisations = Arrays.stream(administrationService.readOrganisations())
                .filter(organisationData -> Objects.nonNull(organisationData.getProviderConfiguration()))
                .filter(organisationData -> providerService.supportsClaims(organisationData.getProviderConfiguration().getProviderId()))
                .toArray(OrganisationData[]::new);

        var model = Map.of("users", users,
                "organisations", organisations,
                "isNew", true
        );
        return new ModelAndView("claims_detail", model);
    }

    @GetMapping("for/{orgId}/{subject}")
    public ModelAndView editClaims(@PathVariable String orgId, @PathVariable String subject) {
        var users = administrationService.readUser(subject);
        var organisations = administrationService.readOrganisation(orgId);
        var claims = administrationService.readClaims(OrganisationSubjectPair.builder()
                        .organisationId(orgId)
                        .subject(subject)
                .build())
                .getClaims();
        var model = Map.of("users", users,
                "organisations", organisations,
                "claims", claims,
                "isNew", false
        );
        return new ModelAndView("claims_detail", model);
    }

    @PostMapping()
    public RedirectView addClaims(
            @RequestParam final String organisationId,
            @RequestParam final String subject,
            @RequestParam(required = false) final String[] claims
    ) {
        if (claims == null) {
            return new RedirectView("/iam/clm/org/" + organisationId);
        }

        administrationService.createClaim(OrganisationSubjectPair.builder()
                .organisationId(organisationId)
                .subject(subject)
                .build(), ClaimData.builder()
                .claims(claims)
                .build());
        return new RedirectView("/iam/clm/org/" + organisationId);
    }

    @PostMapping("for/{orgId}/{subject}")
    public RedirectView updateClaims(
            @PathVariable final String orgId,
            @PathVariable final String subject,
            @RequestParam(required = false) final String[] claims
    ) {
        if (claims == null) {
            administrationService.deleteClaims(OrganisationSubjectPair.builder()
                    .organisationId(orgId)
                    .subject(subject)
                    .build());
            return new RedirectView("/iam/clm/org/" + orgId);
        }

        administrationService.updateClaims(OrganisationSubjectPair.builder()
                .organisationId(orgId)
                .subject(subject)
                .build(), ClaimData.builder()
                .claims(claims)
                .build());
        return new RedirectView("/iam/clm/org/" + orgId);
    }
}
