package dk.acto.fafnir.iam.service.controller;

import dk.acto.fafnir.api.model.ClaimData;
import dk.acto.fafnir.api.model.Slice;
import dk.acto.fafnir.api.service.AdministrationService;
import dk.acto.fafnir.iam.dto.DtoFactory;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

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
    @GetMapping("page/{pageNumber}")
    public ModelAndView getClaimsOverview(@PathVariable Long pageNumber) {
        var maxValue = administrationService.countOrganisations();
        var pageActual = Slice.cropPage(pageNumber, maxValue);
        if (!pageActual.equals(pageNumber - 1)) {
            return new ModelAndView("redirect:/iam/clm/page/" + (pageActual +1));
        }
        var result = administrationService.readClaims((pageActual));
        var orgs = result.getPageData().stream().map(ClaimData::getOrganisationId)
                .distinct().map(administrationService::readOrganisation)
                .collect(Collectors.toList());
        var users = result.getPageData().stream().map(ClaimData::getSubject)
                .distinct().map(administrationService::readUser)
                .collect(Collectors.toList());
        var transformed = dtoFactory.toInfo(result.getPageData(), orgs, users);
        var model = dtoFactory.calculatePageData(pageActual, maxValue, "/iam/org");
        model.put(
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
    public RedirectView addClaims(@ModelAttribute ClaimData source) {
        administrationService.createClaim(source);
        return new RedirectView("/iam/clm/page/0");
    }

    @PutMapping
    public RedirectView updateClaims(@ModelAttribute ClaimData source) {
        administrationService.updateClaims(source);
        return new RedirectView("/iam/clm/page/0");
    }

    @GetMapping("{orgId}/{subject}")
    public ModelAndView getClaims(@PathVariable String orgId, @PathVariable String subject) {
        var result = administrationService.readClaims(orgId, subject);
        var model = Map.of("tableData", result);
        return new ModelAndView("claims_detail", model);
    }
}
