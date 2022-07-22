package dk.acto.fafnir.sso.service.controller;

import dk.acto.fafnir.api.model.conf.FafnirConf;
import dk.acto.fafnir.sso.provider.MitIdProvider;
import dk.acto.fafnir.sso.provider.credentials.TokenCredentials;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import javax.annotation.PostConstruct;

@RestController
@Slf4j
@AllArgsConstructor
@RequestMapping("mitid")
@ConditionalOnBean(MitIdProvider.class)
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
                .code(code)
                .build()).getUrl(fafnirConf));
    }

    @PostConstruct
    private void postConstruct() {
        log.info("Exposing MitId Endpoint...");
    }
}
