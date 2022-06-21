package dk.acto.fafnir.sso.service.controller;

import dk.acto.fafnir.sso.model.conf.FafnirConf;
import dk.acto.fafnir.sso.provider.HazelcastProvider;
import dk.acto.fafnir.sso.provider.credentials.UsernamePasswordCredentials;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import javax.annotation.PostConstruct;
import javax.websocket.server.PathParam;

@Controller
@Slf4j
@RequestMapping("/{orgId}/hazelcast")
@AllArgsConstructor
public class HazelcastController {
    private final HazelcastProvider provider;
    private final FafnirConf fafnirConf;

    @GetMapping
    public RedirectView authenticate() {
        return new RedirectView(provider.authenticate());
    }

    @PostMapping("login")
    public RedirectView callback(@RequestParam String email, @RequestParam String password, @PathParam("orgId") String orgId) {
        return new RedirectView(provider.callback(UsernamePasswordCredentials.builder()
                .username(email)
                .password(password)
                .organisation(orgId)
                .build()).getUrl(fafnirConf));
    }

    @GetMapping(value = "login", produces = MediaType.TEXT_HTML_VALUE)
    public ModelAndView loginView() {
        return new ModelAndView("hazelcast_login");
    }

    @PostConstruct
    private void postConstruct() {
        log.info("Exposing Hazelcast Endpoint...");
    }
}
