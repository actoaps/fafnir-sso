package dk.acto.fafnir.sso.service.controller;

import dk.acto.fafnir.api.model.conf.FafnirConf;
import dk.acto.fafnir.api.service.AdministrationService;
import dk.acto.fafnir.api.service.ProviderService;
import dk.acto.fafnir.sso.provider.HazelcastProvider;
import dk.acto.fafnir.sso.provider.credentials.UsernamePasswordCredentials;
import io.vavr.control.Try;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

@Controller
@Slf4j
@RequestMapping("hazelcast")
@AllArgsConstructor
public class HazelcastController {
    private final HazelcastProvider provider;
    private final FafnirConf fafnirConf;
    private final AdministrationService administrationService;
    private final ProviderService providerService;
    @GetMapping
    public RedirectView authenticate() {
        return new RedirectView(provider.authenticate());
    }

    @PostMapping("login")
    public RedirectView callback(@RequestParam(required = false) Optional<String> email, @RequestParam(required = false) Optional<String> password, @RequestParam(required = false) Optional<String> orgId, RedirectAttributes redirectAttributes) {
        if (email.isPresent() && orgId.isPresent() && password.isPresent()) {
            return new RedirectView(provider.callback(UsernamePasswordCredentials.builder()
                    .username(email.get())
                    .password(password.get())
                    .organisation(orgId.get())
                    .build()).getUrl(fafnirConf));
        }
        email.ifPresent(s -> redirectAttributes.addFlashAttribute("email", s));
        orgId.ifPresent(s -> redirectAttributes.addFlashAttribute("orgId", s));

        return new RedirectView("/hazelcast/login");
    }

    @GetMapping(value = "login", produces = MediaType.TEXT_HTML_VALUE)
    public ModelAndView loginView(final Model input) {
        var email = Optional.ofNullable(input.getAttribute("email"))
                .map(String::valueOf);
        var orgId = Optional.ofNullable(input.getAttribute("orgId"))
                .map(String::valueOf);

        var orgs = email.map(provider::getOrganisations)
                .filter(x -> orgId.isEmpty());
        var org = orgId.flatMap(provider::getOrganisation);
        var model = new TreeMap<String, Object>();
        email.ifPresent(s -> model.put("email", s));
        org.ifPresent(s -> model.put("org", s));
        orgs.ifPresent(s -> model.put("orgs", s));

        return new ModelAndView("hazelcast_login", model);
    }

    @GetMapping(value = "alt", produces = MediaType.TEXT_HTML_VALUE)
    public ModelAndView alternativePicker() {
        return new ModelAndView("organisation_picker", Map.of(
                "loginUrl", "hazelcast/alt",
                "orgs", administrationService.readOrganisations()
        ));
    }

    @PostMapping(value = "alt", produces = MediaType.TEXT_HTML_VALUE)
    public RedirectView alternativeRedirect(@RequestParam String orgId) {
        var org = administrationService.readOrganisation(orgId);
        return new RedirectView("/" + providerService.getAuthenticationUrlForProvider(org.getProviderConfiguration().getProviderId()));
    }

    @PostConstruct
    private void postConstruct() {
        log.info("Exposing Hazelcast Endpoint...");
    }
}
