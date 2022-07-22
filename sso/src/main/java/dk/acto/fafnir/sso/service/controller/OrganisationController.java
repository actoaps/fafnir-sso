package dk.acto.fafnir.sso.service.controller;

import dk.acto.fafnir.api.model.OrganisationSupport;
import dk.acto.fafnir.api.service.AdministrationService;
import dk.acto.fafnir.sso.service.SsoProviderService;
import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import java.util.Map;

@Value
@Controller
@Slf4j
@AllArgsConstructor
@RequestMapping("login")
public class OrganisationController {
    AdministrationService administrationService;
    SsoProviderService ssoProviderService;

    @GetMapping("{page}")
    public ModelAndView organisationPicker(@PathVariable Long page) {
        var orgs = administrationService.readOrganisations(page);
        return new ModelAndView("organisation_picker", Map.of("orgs", orgs));
    }

    @PostMapping
    public RedirectView organisationRedirect(@RequestParam String orgId) {
        var myOrganisation = administrationService.readOrganisation(orgId);
        var pc = myOrganisation.getProviderConfiguration();
        var pi = ssoProviderService.getProviderMetaData(pc.getProviderId());
        if (pi.getOrganisationSupport().equals(OrganisationSupport.FAFNIR)) {
            return new RedirectView("/"
                    + pc.getProviderId()
                    + "/"
                    + myOrganisation.getOrganisationId());
        } else {
            return new RedirectView("/"
                    + pc.getProviderId());
        }
    }
}
