package dk.acto.fafnir.iam.service.controller;

import dk.acto.fafnir.api.exception.OrganisationUpdateFailed;
import dk.acto.fafnir.api.model.OrganisationData;
import dk.acto.fafnir.api.model.Slice;
import dk.acto.fafnir.api.service.AdministrationService;
import dk.acto.fafnir.iam.dto.DtoFactory;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.time.Instant;
import java.util.Map;

@Controller
@Slf4j
@AllArgsConstructor
@RequestMapping("/iam/org")
public class OrganisationController {
    private final AdministrationService administrationService;
    private final DtoFactory dtoFactory;

    @GetMapping("page/{pageNumber}")
    public ModelAndView getOrganisationOverview(@PathVariable Long pageNumber) {
        var maxValue = administrationService.countOrganisations();
        var pageActual = Slice.cropPage(pageNumber, maxValue);
        if (!pageActual.equals(pageNumber - 1)) {
            return new ModelAndView("redirect:/iam/org/page/" + (pageActual +1));
        }
        var result = administrationService.readOrganisations(pageActual);
        var model = dtoFactory.calculatePageData(pageActual, maxValue, "/iam/org");
        model.put("tableData", result.getPageData());
        return new ModelAndView("organisation_overview", model);
    }

    @GetMapping("{orgId}")
    public ModelAndView getOrganisationDetail(@PathVariable String orgId) {
        var result = administrationService.readOrganisation(orgId);
        var model = Map.of(
                "tableData", result,
                "isNew", false
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
                "isNew", true
        );
        return new ModelAndView("organisation_detail", model);
    }

    @PostMapping("{orgId}")
    public ModelAndView updateOrganisation(@ModelAttribute OrganisationData org, @PathVariable String orgId) {
        if(!orgId.equals(org.getOrganisationId())) {
            throw new OrganisationUpdateFailed();
        }
        administrationService.updateOrganisation(org);
        return new ModelAndView("redirect:/iam/org/page/1");
    }

    @PostMapping
    public ModelAndView createOrganisation(@ModelAttribute OrganisationData org) {
        administrationService.createOrganisation(org);
        return new ModelAndView("redirect:/iam/org/page/1");
    }
}
