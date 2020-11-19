package dk.acto.fafnir.server.services.controller;

import dk.acto.fafnir.server.model.conf.FafnirConf;
import dk.acto.fafnir.server.providers.AppleProvider;
import dk.acto.fafnir.server.providers.credentials.TokenCredentials;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import javax.annotation.PostConstruct;

@RestController
@Slf4j
@AllArgsConstructor
@RequestMapping("apple")
@ConditionalOnProperty(name = {"APPLE_AID", "APPLE_SECRET"})
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
                .token(code)
                .build()).getUrl(fafnirConf));
    }

    @PostConstruct
    private void postConstruct() {
        log.info("Exposing Apple Endpoint...");
    }
}
