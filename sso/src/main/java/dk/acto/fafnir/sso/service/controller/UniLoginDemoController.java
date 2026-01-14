package dk.acto.fafnir.sso.service.controller;

import dk.acto.fafnir.sso.provider.unilogin.Institution;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

/**
 * Demo controller to preview the institution chooser template with mock data.
 * This controller is always available, regardless of UniLogin configuration.
 * Access at: http://localhost:8080/unilogin-demo
 */
@Controller
@Slf4j
@RequestMapping("unilogin-demo")
public class UniLoginDemoController {

    /**
     * Demo endpoint to preview the institution chooser template with mock data.
     * Access at: http://localhost:8080/unilogin-demo
     */
    @GetMapping
    public String demo(Model model) {
        log.info("Demo endpoint called - showing institution chooser with mock data");
        
        // Create mock institutions with various roles
        List<Institution> mockInstitutions = List.of(
            new Institution("999904", "TEST FOLKESKOLE", List.of("Lærer", "Pædagog")),
            new Institution("R00147", "Test Skole", List.of("Lærer", "Leder")),
            new Institution("R00146", "Test Efterskole", List.of("Pædagog", "Vikar")),
            new Institution("R00213", "ET Skole", List.of("Medarbejder")),
            new Institution("999958", "Top Privatskolen", List.of("Lærer", "Pædagog", "Ledelse"))
        );
        
        model.addAttribute("user", "1000000158");
        model.addAttribute("institutionList", mockInstitutions);
        
        return "uni_login_oidc_institution_chooser_da";
    }

    @PostConstruct
    private void postConstruct() {
        log.info("Exposing UniLogin Demo Endpoint at /unilogin-demo");
    }
}
