package dk.acto.fafnir.iam.service.controller;

import dk.acto.fafnir.api.model.OrganisationData;
import dk.acto.fafnir.api.service.AdministrationService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import java.time.Instant;
import java.util.Map;

@Controller
@Slf4j
@AllArgsConstructor
@RequestMapping("iam/org")
public class OrganisationController {
    private final AdministrationService administrationService;

    @GetMapping("page/{pageNumber}")
    public ModelAndView getOrganisationOverview(@PathVariable Long pageNumber) {
        var result = administrationService.readOrganisations(pageNumber);
        var model = Map.of("page", pageNumber,
                "pages", result.getTotalPages(),
                "tableData", result.getPageData());
        return new ModelAndView("organisation_overview", model);
    }

    @GetMapping("{orgId}")
    public ModelAndView getOrganisationDetail(@PathVariable String orgId) {
        var result = administrationService.readOrganisation(orgId);
        var model = Map.of(
                "tableData", result,
                "action", "Edit ",
                "verb", "put"
        );
        return new ModelAndView("organisation_detail", model);
    }

    @GetMapping
    public ModelAndView getEmptyOrganisationDetail() {
        var result = OrganisationData.builder()
                .contactEmail("")
                .organisationName("")
                .organisationId("")
                .created(Instant.now())
                .build();
        var model = Map.of(
                "tableData", result,
                "action", "Create ",
                "verb", "post"
        );
        return new ModelAndView("organisation_detail", model);
    }

    @PutMapping
    public RedirectView updateOrganisation(@ModelAttribute OrganisationData org) {
        administrationService.updateOrganisation(org);
        return new RedirectView("/iam/org/page/0");
    }

    @PostMapping
    public RedirectView createOrganisation(@ModelAttribute OrganisationData org) {
        administrationService.createOrganisation(org);
        return new RedirectView("/iam/org/page/0");
    }
}
