package dk.acto.fafnir.sso.service.controller;

import dk.acto.fafnir.api.model.conf.FafnirConf;
import dk.acto.fafnir.sso.provider.UniLoginLightweightProvider;
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
@AllArgsConstructor
@RequestMapping("unilogin-Lightweight")
@ConditionalOnProperty(name = {"UNILOGIN_CLIENT_ID", "UNILOGIN_SECRET"})
public class UniLoginLightweightController {
    private final UniLoginLightweightProvider provider;
    private final FafnirConf uniloginConf;

    @GetMapping
    public RedirectView authenticate() {
        return new RedirectView(provider.authenticate());
    }

    @PostMapping("callback")
    public RedirectView callback(@RequestParam("id_token") String code) {
        return new RedirectView(provider.callback(TokenCredentials.builder()
            .code(code)
            .build()).getUrl(uniloginConf));
    }

    @PostConstruct
    private void postConstruct() {
        log.info("Exposing Unilogin OIDC Lightweight Endpoint...");
    }
}
