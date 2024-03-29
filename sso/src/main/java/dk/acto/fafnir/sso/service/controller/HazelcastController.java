package dk.acto.fafnir.sso.service.controller;

import dk.acto.fafnir.api.model.conf.FafnirConf;
import dk.acto.fafnir.api.model.conf.HazelcastConf;
import dk.acto.fafnir.api.service.AdministrationService;
import dk.acto.fafnir.api.service.ProviderService;
import dk.acto.fafnir.sso.service.ServiceHelper;
import dk.acto.fafnir.sso.dto.HazelcastLoginInfo;
import dk.acto.fafnir.sso.dto.LoginResponseInfo;
import dk.acto.fafnir.sso.provider.HazelcastProvider;
import dk.acto.fafnir.sso.provider.credentials.UsernamePasswordCredentials;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

@Controller
@Slf4j
@RequestMapping("hazelcast")
@AllArgsConstructor
public class HazelcastController {
    private final HazelcastProvider provider;
    private final HazelcastConf hazelcastConf;
    private final FafnirConf fafnirConf;
    private final AdministrationService administrationService;
    private final ProviderService providerService;

    @GetMapping
    public RedirectView authenticate() {
        return new RedirectView(provider.authenticate());
    }

    @PostMapping(value = "login", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public LoginResponseInfo login(@RequestBody HazelcastLoginInfo info) {
        return provider.callback(info);
    }

    @PostMapping(value = "login")
    public RedirectView callback(@RequestParam(required = false) Optional<String> email, @RequestParam(required = false) Optional<String> password,
                                 @RequestParam(required = false) Optional<String> orgId, RedirectAttributes redirectAttributes) {
        if (email.isPresent() && orgId.isPresent() && password.isPresent()) {
            return new RedirectView(provider.callback(UsernamePasswordCredentials.builder()
                    .username(email.get())
                    .password(password.get())
                    .organisation(orgId.get())
                    .build()).getUrl(fafnirConf));
        }
        email.ifPresent(s -> redirectAttributes.addFlashAttribute("email", s));
        orgId.ifPresent(s -> redirectAttributes.addFlashAttribute("orgId", s));

        return new RedirectView(fafnirConf.buildUrl("/hazelcast/login"));
    }

	@GetMapping(value = "login", produces = MediaType.TEXT_HTML_VALUE)
    public ModelAndView loginView(final Model input, @RequestHeader("Accept-Language") String locale) {
        var email = Optional.ofNullable(input.getAttribute("email"))
                .map(String::valueOf);
        var orgId = Optional.ofNullable(input.getAttribute("orgId"))
                .map(String::valueOf);

        var orgs = email.map(provider::getOrganisations)
                .filter(x -> orgId.isEmpty());
        var org = orgId.flatMap(provider::getOrganisation);
        var model = new TreeMap<String, Object>();
        model.put("usernameIsEmail", hazelcastConf.isUsernameIsEmail());
        email.ifPresent(s -> model.put("email", s));
        org.ifPresent(s -> model.put("org", s));
        orgs.ifPresent(s -> {
            if (s.size() == 1) {
                model.put("org", s.get(0));
            }
            model.put("orgs", s);
        });

        return new ModelAndView("hazelcast_login" + ServiceHelper.getLocaleStr(locale, "da", "en"), model);
    }

    @GetMapping(value = "alt", produces = MediaType.TEXT_HTML_VALUE)
    public ModelAndView alternativePicker(@RequestHeader("Accept-Language") String locale) {
        return new ModelAndView("organisation_picker" + ServiceHelper.getLocaleStr(locale, "da", "en"), Map.of(
                "loginUrl", fafnirConf.buildUrl("/hazelcast/alt"),
                "orgs", provider.getOrganisationsForProvider()
        ));
    }

    @PostMapping(value = "alt", produces = MediaType.TEXT_HTML_VALUE)
    public RedirectView alternativeRedirect(@RequestParam String orgId) {
        var org = administrationService.readOrganisation(orgId);
        return new RedirectView(providerService.getAuthenticationUrlForProvider(org.getProviderConfiguration().getProviderId()));
    }

    @PostConstruct
    private void postConstruct() {
        log.info("Exposing Hazelcast Endpoint...");
    }
}
