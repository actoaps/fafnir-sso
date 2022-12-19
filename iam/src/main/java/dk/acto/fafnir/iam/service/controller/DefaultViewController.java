package dk.acto.fafnir.iam.service.controller;

import dk.acto.fafnir.api.service.AdministrationService;
import lombok.AllArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.view.RedirectView;

@Controller
@AllArgsConstructor
@RequestMapping
@PreAuthorize("hasAuthority(T(dk.acto.fafnir.iam.security.IAMRoles).FAFNIR_ADMIN.toString())")
public class DefaultViewController {
    private final AdministrationService administrationService;

    @GetMapping({"", "/iam"})
    public RedirectView pickDefaultView() {
        if (administrationService.countOrganisations() < 1) {
            return new RedirectView("/iam/org");
        }
        return new RedirectView("/iam/org/page/1");
    }
}
