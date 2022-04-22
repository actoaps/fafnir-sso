package dk.acto.fafnir.iam.service.controller;

import dk.acto.fafnir.api.service.AdministrationService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.view.RedirectView;

@Controller
@Slf4j
@AllArgsConstructor
@RequestMapping
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
