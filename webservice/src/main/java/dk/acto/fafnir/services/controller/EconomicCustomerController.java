package dk.acto.fafnir.services.controller;

import dk.acto.fafnir.model.conf.FafnirConf;
import dk.acto.fafnir.providers.EconomicCustomerProvider;
import dk.acto.fafnir.providers.credentials.UsernamePassword;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.http.HttpServletResponse;

@RestController
@Slf4j
@AllArgsConstructor
@ConditionalOnBean(EconomicCustomerProvider.class)
@RequestMapping("economic")
public class EconomicCustomerController{
    private final EconomicCustomerProvider provider;
    private final FafnirConf fafnirConf;

    @GetMapping
    public RedirectView authenticate(HttpServletResponse response) {
        return new RedirectView(provider.authenticate());
    }

    @PostMapping("login")
    public RedirectView callback(HttpServletResponse response, @RequestParam String email, @RequestParam String customerNumber) {
        return new RedirectView(provider.callback(UsernamePassword.builder()
                .username(email)
                .password(customerNumber)
                .build()).getUrl(fafnirConf));
    }

    @GetMapping(value = "login", produces = MediaType.TEXT_HTML_VALUE)
    public ModelAndView loginView() {
        return new ModelAndView("thymeleaf/Credentials.thymeleaf.html");
    }
}
