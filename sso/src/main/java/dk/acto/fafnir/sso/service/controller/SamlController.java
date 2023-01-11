package dk.acto.fafnir.sso.service.controller;

import dk.acto.fafnir.api.model.conf.FafnirConf;
import dk.acto.fafnir.sso.provider.SamlProvider;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import java.util.Map;

@Slf4j
@Controller
@AllArgsConstructor
@RequestMapping("saml")
@ConditionalOnBean(SamlProvider.class)
public class SamlController {
    private final SamlProvider provider;
    private final FafnirConf fafnirConf;

    @GetMapping
    public RedirectView authenticate() {
        return new RedirectView(provider.authenticate());
    }

    @GetMapping("callback")
    public RedirectView callback(@AuthenticationPrincipal Saml2AuthenticatedPrincipal principal) {
        return new RedirectView(provider.callback(
                principal
        ).getUrl(fafnirConf));
    }

    @GetMapping(value = "login", produces = MediaType.TEXT_HTML_VALUE)
    public ModelAndView login() {
        return new ModelAndView("organisation_picker", Map.of(
                "loginUrl", provider.authenticate(),
                "orgs", provider.getOrgsForProvider()
        ));
    }

    @PostMapping(value = "login", produces = MediaType.TEXT_HTML_VALUE)
    public RedirectView loginRedirect(@RequestParam String orgId) {
        var registrationId = provider.getSamlRegistrationId(orgId);
        return new RedirectView(fafnirConf.buildUrl("/saml2/authenticate/" + registrationId));
    }

    @PostConstruct
    private void postConstruct() {
        log.info("Exposing SAML Endpoint...");
    }
}
