package dk.acto.fafnir.sso.service.controller;

import dk.acto.fafnir.api.model.conf.FafnirConf;
import dk.acto.fafnir.sso.provider.MicrosoftIdentityProvider;
import dk.acto.fafnir.sso.provider.credentials.TokenCredentials;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;

@Controller
@Slf4j
@RequestMapping("msidentity")
@AllArgsConstructor
@ConditionalOnProperty(name = {"MSID_AID", "MSID_SECRET", "MSID_TENANT"})
public class MicrosoftIdentityController {
    private final MicrosoftIdentityProvider provider;
    private final FafnirConf fafnirConf;

    @GetMapping
    public RedirectView authenticate() {
        return new RedirectView(provider.authenticate());
    }

    @PostMapping("callback")
    public RedirectView callback(@RequestParam("id_token") String idToken) {
        return new RedirectView(provider.callback(TokenCredentials.builder()
                .code(idToken)
                .build()).getUrl(fafnirConf));
    }

    @PostConstruct
    private void postConstruct() {
        log.info("Exposing Microsoft Identity Endpoint...");
    }
}
