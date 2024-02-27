package dk.acto.fafnir.sso.service.controller;

import dk.acto.fafnir.api.model.conf.FafnirConf;
import dk.acto.fafnir.sso.provider.UniLoginLightweightProvider;
import dk.acto.fafnir.sso.provider.unilogin.UniloginTokenCredentials;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

@Controller
@Slf4j
@AllArgsConstructor
@RequestMapping("unilogin-lightweight")
@ConditionalOnProperty(name = {"UL_CLIENT_ID", "UL_SECRET", "FAFNIR_URL"})
public class UniLoginLightweightController {
    private final UniLoginLightweightProvider provider;
    private final FafnirConf uniloginConf;

    @GetMapping
    public RedirectView authenticate(HttpSession session) throws NoSuchAlgorithmException {
        return new RedirectView(provider.authenticate(session));
    }

    @PostMapping("callback")
    public RedirectView callback(@RequestParam("code") String code, HttpSession session) throws IOException {
        return new RedirectView(provider.callback(UniloginTokenCredentials.builder()
            .code(code)
            .build(), session).getUrl(uniloginConf));
    }

    @PostConstruct
    private void postConstruct() {
        log.info("Exposing Unilogin lightweight OIDC Endpoint...");
    }
}
