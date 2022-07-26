package dk.acto.fafnir.sso.service.controller;

import dk.acto.fafnir.api.model.conf.FafnirConf;
import dk.acto.fafnir.sso.provider.AppleProvider;
import dk.acto.fafnir.sso.provider.credentials.TokenCredentials;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import javax.annotation.PostConstruct;

@Controller
@Slf4j
@AllArgsConstructor
@RequestMapping("apple")
@ConditionalOnBean(AppleProvider.class)
public class AppleController {
    private final AppleProvider provider;
    private final FafnirConf fafnirConf;

    @GetMapping
    public RedirectView authenticate() {
        return new RedirectView(provider.authenticate());
    }

    @PostMapping("callback")
    public RedirectView callback(@RequestParam("id_token") String code) {
        return new RedirectView(provider.callback(TokenCredentials.builder()
                .code(code)
                .build()).getUrl(fafnirConf));
    }

    @PostConstruct
    private void postConstruct() {
        log.info("Exposing Apple Endpoint...");
    }
}
