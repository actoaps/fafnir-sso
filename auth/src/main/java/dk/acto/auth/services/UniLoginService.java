package dk.acto.auth.services;

import dk.acto.auth.ActoConf;
import dk.acto.auth.providers.UniLoginProvider;
import dk.acto.auth.providers.validators.UniLoginValidator;
import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.Map;

@RestController
@Slf4j
@RequestMapping("unilogin")
public class UniLoginService implements Callback3Service {
	private final UniLoginProvider provider;
	private final ActoConf actoConf;
	
	@Autowired
	public UniLoginService(UniLoginProvider provider, @Validated(UniLoginValidator.class) ActoConf actoConf) {
		this.provider = provider;
		this.actoConf = actoConf;
	}
	
	
	@GetMapping
	public void authenticate(HttpServletResponse response) {
		Try.of(() -> ServiceHelper.functionalRedirectTo(response, provider::authenticate));
	}
	
	@GetMapping("callback")
	public void callback(HttpServletResponse response, @RequestParam String user, @RequestParam String timestamp, @RequestParam String auth) {
		Try.of(() -> ServiceHelper.functionalRedirectTo(response, () -> provider.callback(user, timestamp, auth)));
	}
	
	@GetMapping("org")
	public String getOrg(@RequestParam String user, @RequestParam String timestamp, @RequestParam String auth, @RequestHeader("Accept-Language") String locale) {
		var institutionList = provider.getInstitutionList(user);
		var model = Map.of(
				"user", user,
				"timestamp", timestamp,
				"auth", auth,
				"institutionList", institutionList
		);
		return "/thymeleaf/ChooseInstitutionUni-Login" +
				ServiceHelper.getLocaleStr(locale, "da", "en") +
				".thymeleaf";
	}
	
	@PostMapping("org")
	public void postOrg(HttpServletResponse response, @RequestParam String user, @RequestParam String timestamp, @RequestParam String auth, @RequestParam String institution) {
		Try.of(() -> ServiceHelper.functionalRedirectTo(response, () -> provider.callbackWithInstitution(user, timestamp, auth, institution)));
	}
}
