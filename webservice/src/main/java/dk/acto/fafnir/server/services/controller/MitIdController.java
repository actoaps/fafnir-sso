package dk.acto.fafnir.server.services.controller;

import dk.acto.fafnir.server.model.conf.FafnirConf;
import dk.acto.fafnir.server.providers.AppleProvider;
import dk.acto.fafnir.server.providers.MitIdProvider;
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
@RequestMapping("mitid")
@ConditionalOnProperty(name = {"MITID_AID", "MITID_SECRET", "MITID_AUTHORITY_URL"})
public class MitIdController {
    private final MitIdProvider provider;
    private final FafnirConf fafnirConf;

    @GetMapping
    public RedirectView authenticate() {
        return new RedirectView(provider.authenticate());
    }

    @GetMapping("callback")
    public RedirectView callback(@RequestParam String code) {
        return new RedirectView(provider.callback(TokenCredentials.builder()
                .token(code)
                .build()).getUrl(fafnirConf));
    }

    @PostConstruct
    private void postConstruct() {
        log.info("Exposing MitId Endpoint...");
    }
}
