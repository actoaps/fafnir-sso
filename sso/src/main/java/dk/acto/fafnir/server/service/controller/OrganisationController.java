package dk.acto.fafnir.server.service.controller;

import dk.acto.fafnir.api.service.AdministrationService;
import dk.acto.fafnir.server.service.ProviderService;
import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    ProviderService providerService;

    @GetMapping
    public ModelAndView organisationPicker() {
        var orgs = administrationService.readOrganisations();
        return new ModelAndView("Organisation.thymeleaf.html", Map.of("orgs", orgs));
    }

    @PostMapping
    public RedirectView organisationRedirect(@RequestParam String orgId) {
        var myOrganisation = administrationService.readOrganisation(orgId);
        var orgSupport = providerService.providerSupportsOrganisations(myOrganisation.getProvider());
        var sb = new StringBuilder();
        if (orgSupport) {
            sb.append(myOrganisation.getOrganisationId()).append("/");
        }
        sb.append(myOrganisation.getProvider());
        return new RedirectView(sb.toString());
    }
}
