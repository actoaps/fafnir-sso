package dk.acto.fafnir.sso.service.controller;

import dk.acto.fafnir.api.model.FailureReason;
import dk.acto.fafnir.api.model.conf.FafnirConf;
import dk.acto.fafnir.sso.provider.UniLoginProvider;
import dk.acto.fafnir.sso.provider.unilogin.UniloginTokenCredentials;
import io.vavr.control.Try;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;

@Controller
@Slf4j
@AllArgsConstructor
@RequestMapping("unilogin")
@ConditionalOnProperty(name = {"UL_CLIENT_ID", "UL_SECRET", "FAFNIR_URL", "UL_WS_USER", "UL_WS_PASS"})
public class UniLoginController {
    private final UniLoginProvider provider;
    private final FafnirConf uniloginConf;

    @GetMapping
    public RedirectView authenticate(HttpSession session) throws NoSuchAlgorithmException {
        return new RedirectView(provider.authenticate(session));
    }

    @PostMapping("callback")
    public RedirectView callback(@RequestParam("code") String code, HttpSession session) throws IOException {
        return new RedirectView(provider.callback(UniloginTokenCredentials.builder()
            .code(code)
            .build(), session).getUrl(uniloginConf));
    }

    @GetMapping("org")
    public String getOrg(@RequestParam String user, Model model, HttpServletResponse response, HttpSession session) throws IOException {
        log.info("getOrg called for user: {}", user);
        
        // Try to get institutions from UserInfo in session first, fallback to deprecated web service
        var institutionList = Try.of(() -> provider.getInstitutionListFromSession(user, session)).getOrElse(Collections.emptyList());

        log.info("Retrieved {} institution(s) for user: {}", institutionList.size(), user);
        
        // Log each institution's details
        for (int i = 0; i < institutionList.size(); i++) {
            var inst = institutionList.get(i);
            log.info("Institution[{}]: id={}, name={}, roles={}, class={}", 
                i, inst.id, inst.name, inst.roles, inst.getClass().getName());
            
            // Verify fields are accessible
            try {
                var idValue = inst.id;
                var nameValue = inst.name;
                var rolesValue = inst.roles;
                log.debug("Field access test - id: {}, name: {}, roles: {}", idValue, nameValue, rolesValue);
            } catch (Exception e) {
                log.error("Error accessing Institution fields", e);
            }
        }

        if (institutionList.isEmpty()) {
            log.warn("No institutions found for user: {}, redirecting to failure URL", user);
            response.sendRedirect(provider.getFailureUrl(FailureReason.CONNECTION_FAILED));
            return null;
        } else {
            log.info("Adding {} institution(s) to model for template rendering", institutionList.size());
            model.addAttribute("user", user);
            model.addAttribute("institutionList", institutionList);
            log.info("Model attributes set - user: {}, institutionList size: {}", user, institutionList.size());
            return "uni_login_oidc_institution_chooser_da";
        }
    }


    @PostMapping("org")
    @ResponseBody
    public RedirectView postOrg(@RequestParam String user, @RequestParam String institution, HttpSession session) {
        return new RedirectView(provider.callbackWithInstitution(user, institution, null, session).getUrl(uniloginConf));
    }


    @PostConstruct
    private void postConstruct() {
        log.info("Exposing Unilogin OIDC Endpoint...");
    }
}
