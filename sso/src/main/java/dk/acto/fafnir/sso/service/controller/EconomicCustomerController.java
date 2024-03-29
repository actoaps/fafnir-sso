package dk.acto.fafnir.sso.service.controller;

import dk.acto.fafnir.api.model.conf.FafnirConf;
import dk.acto.fafnir.sso.provider.EconomicCustomerProvider;
import dk.acto.fafnir.sso.provider.credentials.UsernamePasswordCredentials;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;


@Controller
@Slf4j
@AllArgsConstructor
@RequestMapping("economic")
@ConditionalOnProperty(name = {"ECONOMIC_AST", "ECONOMIC_AGT"})
public class EconomicCustomerController {
    private final EconomicCustomerProvider provider;
    private final FafnirConf fafnirConf;

    @GetMapping
    public RedirectView authenticate() {
        return new RedirectView(fafnirConf.getUrl() + provider.authenticate());
    }

    @PostMapping("login")
    public RedirectView callback(@RequestParam String email, @RequestParam String customerNumber) {
        return new RedirectView(provider.callback(UsernamePasswordCredentials.builder()
                .username(email)
                .password(customerNumber)
                .build()).getUrl(fafnirConf));
    }

    @GetMapping(value = "login", produces = MediaType.TEXT_HTML_VALUE)
    public ModelAndView loginView() {
        return new ModelAndView("economic_login");
    }

    @PostConstruct
    private void postConstruct() {
        log.info("Exposing Economic Endpoint...");
    }
}
