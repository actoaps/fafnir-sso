package dk.acto.fafnir.server.service.controller;

import dk.acto.fafnir.server.model.conf.FafnirConf;
import dk.acto.fafnir.server.provider.EconomicCustomerProvider;
import dk.acto.fafnir.server.provider.credentials.UsernamePasswordCredentials;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletResponse;

@RestController
@Slf4j
@AllArgsConstructor
@RequestMapping("economic")
@ConditionalOnProperty(name = {"ECONOMIC_AST", "ECONOMIC_AGT"})
public class EconomicCustomerController{
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
        return new ModelAndView("thymeleaf/Credentials.thymeleaf.html");
    }

    @PostConstruct
    private void postConstruct() {
        log.info("Exposing Economic Endpoint...");
    }
}
