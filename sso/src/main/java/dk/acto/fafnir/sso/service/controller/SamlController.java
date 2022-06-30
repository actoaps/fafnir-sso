package dk.acto.fafnir.sso.service.controller;

import dk.acto.fafnir.api.model.conf.FafnirConf;
import dk.acto.fafnir.sso.provider.SamlProvider;
import dk.acto.fafnir.sso.provider.credentials.SamlCredentials;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import javax.annotation.PostConstruct;

@Slf4j
@Controller
@AllArgsConstructor
@ConditionalOnBean(SamlProvider.class)
public class SamlController {
    private final SamlProvider provider;
    private final FafnirConf fafnirConf;

    @GetMapping("{orgId}/saml")
    public RedirectView authenticate() {
        return new RedirectView(provider.authenticate());
    }

    @GetMapping("saml/callback")
    public RedirectView callback(@AuthenticationPrincipal Saml2AuthenticatedPrincipal principal) {
        return new RedirectView(provider.callback(SamlCredentials.builder()
                        .registrationId(principal.getRelyingPartyRegistrationId())
                .email(principal.getFirstAttribute("email"))
                .build()).getUrl(fafnirConf));
    }

    @GetMapping(value = "{orgId}/saml/login", produces = MediaType.TEXT_HTML_VALUE)
    public ModelAndView login(@PathVariable("orgId") String orgId) {
        var registrationId = provider.getSamlRegistrationIds(orgId);
        return new ModelAndView("redirect:/saml2/authenticate/" + registrationId);
    }

    @PostConstruct
    private void postConstruct() {
        log.info("Exposing SAML Endpoint...");
    }
}
