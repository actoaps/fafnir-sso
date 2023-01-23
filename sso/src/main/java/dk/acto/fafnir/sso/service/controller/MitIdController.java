package dk.acto.fafnir.sso.service.controller;

import dk.acto.fafnir.api.model.conf.FafnirConf;
import dk.acto.fafnir.sso.provider.MitIdProvider;
import dk.acto.fafnir.sso.provider.credentials.TokenCredentials;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;

@Controller
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
                .code(code)
                .build()).getUrl(fafnirConf));
    }

    @PostConstruct
    private void postConstruct() {
        log.info("Exposing MitId Endpoint...");
    }
}
