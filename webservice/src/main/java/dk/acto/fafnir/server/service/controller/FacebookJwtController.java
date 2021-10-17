package dk.acto.fafnir.server.service.controller;

import dk.acto.fafnir.server.model.UserData;
import dk.acto.fafnir.server.model.conf.FafnirConf;
import dk.acto.fafnir.server.provider.JwtProvider;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequestMapping("facebook/jwt")
@AllArgsConstructor
@ConditionalOnProperty(name = {"FACEBOOK_AID", "FACEBOOK_SECRET"})
public class FacebookJwtController {

    private final JwtProvider provider;
    private final FafnirConf fafnirConf;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public String getFacebookLoginJwt(@RequestBody UserData userData) {
        return provider.callback(userData).getUrl(fafnirConf);
    }
}
