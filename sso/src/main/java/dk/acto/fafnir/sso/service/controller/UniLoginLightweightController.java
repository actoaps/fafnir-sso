package dk.acto.fafnir.sso.service.controller;

import dk.acto.fafnir.api.model.FailureReason;
import dk.acto.fafnir.api.model.conf.FafnirConf;
import dk.acto.fafnir.sso.provider.UniLoginHelper;
import dk.acto.fafnir.sso.provider.UniLoginLightweightProvider;
import dk.acto.fafnir.sso.provider.unilogin.UniloginTokenCredentials;
import dk.acto.fafnir.sso.service.ServiceHelper;
import io.vavr.control.Try;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Map;

@Controller
@Slf4j
@AllArgsConstructor
@RequestMapping("unilogin-lightweight")
@ConditionalOnProperty(name = {"UL_CLIENT_ID", "UL_SECRET", "FAFNIR_URL", "UL_WS_USER", "UL_WS_PASS"})
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

    @GetMapping("org")
    public String getOrg(@RequestParam String userId, @RequestHeader("Accept-Language") String locale, Model model, HttpServletResponse response) throws IOException {
        var institutionList = Try.of(() -> provider.getInstitutionList(userId)).getOrElse(Collections.emptyList());
        if (institutionList.isEmpty()) {
            response.sendRedirect(provider.getFailureUrl(FailureReason.CONNECTION_FAILED));
            return null; // Stop further execution since redirect is sent
        } else {
            model.addAllAttributes(
                Map.of(
                    UniLoginHelper.USER_ID, userId,
                    "institutionList", institutionList
                )
            );
            return "ChooseInstitutionUni-Login" + ServiceHelper.getLocaleStr(locale, "da", "en") + ".thymeleaf";
        }
    }

    @PostMapping("org")
    @ResponseBody
    public void postOrg(HttpServletResponse response,
                        @RequestParam String user,
                        @RequestParam String institution) {
        Try.of(() -> ServiceHelper.functionalRedirectTo(response, () -> provider.callbackWithInstitution(user, institution).toString()));
    }


    @PostConstruct
    private void postConstruct() {
        log.info("Exposing Unilogin lightweight OIDC Endpoint...");
    }
}
