package dk.acto.fafnir.iam.service.controller;

import dk.acto.fafnir.api.service.AdministrationService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

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
        var model = Map.of("tableData", result);
        return new ModelAndView("organisation_detail", model);
    }
}
