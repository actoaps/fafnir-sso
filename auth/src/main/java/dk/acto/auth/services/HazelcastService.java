package dk.acto.auth.services;

import dk.acto.auth.providers.HazelcastProvider;
import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletResponse;

@RestController
@Slf4j
@RequestMapping("hazelcast")
public class HazelcastService  implements Callback2Service {
    private final HazelcastProvider provider;

    public HazelcastService(HazelcastProvider provider) {
        this.provider = provider;
    }

    @Override
    @GetMapping
    public void authenticate(HttpServletResponse response) {
        Try.of(() -> ServiceHelper.functionalRedirectTo(response, provider::authenticate));
    }

    @PostMapping("login")
    public void callback(HttpServletResponse response, @RequestParam String email, @RequestParam String password) {
        Try.of(() -> ServiceHelper.functionalRedirectTo(response, () -> provider.callback(email, password)));
    }

    @GetMapping(value = "login", produces = MediaType.TEXT_HTML_VALUE)
    public ModelAndView loginView() {
        return new ModelAndView("thymeleaf/Hazelcast.thymeleaf.html");
    }
}
