package dk.acto.auth.services.controller;

import dk.acto.auth.model.conf.EconomicConf;
import dk.acto.auth.providers.EconomicCustomerProvider;
import dk.acto.auth.providers.credentials.UsernamePassword;
import dk.acto.auth.services.ServiceHelper;
import io.vavr.control.Try;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletResponse;

@RestController
@Slf4j
@AllArgsConstructor
@ConditionalOnBean(EconomicCustomerProvider.class)
@RequestMapping("economic")
public class EconomicCustomerController{
    private final EconomicCustomerProvider provider;

    @GetMapping
    public void authenticate(HttpServletResponse response) {
        Try.of(() -> ServiceHelper.functionalRedirectTo(response, provider::authenticate));
    }

    @PostMapping("login")
    public void callback(HttpServletResponse response, @RequestParam String email, @RequestParam String customerNumber) {
        Try.of(() -> ServiceHelper.functionalRedirectTo(response, () -> provider.callback(UsernamePassword.builder()
                .username(email)
                .password(customerNumber)
                .build())));
    }

    @GetMapping(value = "login", produces = MediaType.TEXT_HTML_VALUE)
    public ModelAndView loginView() {
        return new ModelAndView("thymeleaf/Credentials.thymeleaf.html");
    }
}
