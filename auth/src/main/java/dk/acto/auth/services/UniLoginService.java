package dk.acto.auth.services;

import dk.acto.auth.ActoConf;
import dk.acto.auth.providers.UniLoginConstants;
import dk.acto.auth.providers.UniLoginProvider;
import dk.acto.auth.providers.validators.UniLoginValidator;
import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

@Controller
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
	@ResponseBody
	public void authenticate(HttpServletResponse response) {
		Try.of(() -> ServiceHelper.functionalRedirectTo(response, provider::authenticate));
	}
	
	@GetMapping("callback")
	@ResponseBody
	public void callback(HttpServletResponse response, @RequestParam String user, @RequestParam String timestamp, @RequestParam String auth) {
		Try.of(() -> ServiceHelper.functionalRedirectTo(response, () -> provider.callback(user, timestamp, auth)));
	}
	
	@GetMapping("org")
	public String getOrg(@RequestParam String user, @RequestParam String timestamp, @RequestParam String auth, @RequestHeader("Accept-Language") String locale, Model model) {
		var institutionList = provider.getInstitutionList(user);
		model.addAllAttributes(
				new HashMap<String, Object>() {{
					put(UniLoginConstants.USER_ID, user);
					put(UniLoginConstants.TIMESTAMP, timestamp);
					put(UniLoginConstants.STATE_AUTH_ENCODED, auth);
					put("institutionList", institutionList);
				}}
		);
		return "thymeleaf/ChooseInstitutionUni-Login" +
				ServiceHelper.getLocaleStr(locale, "da", "en") +
				".thymeleaf";
	}
	
	@PostMapping("org")
	@ResponseBody
	public void postOrg(HttpServletResponse response, @RequestParam String user, @RequestParam String timestamp, @RequestParam String auth, @RequestParam String institution) {
		Try.of(() -> ServiceHelper.functionalRedirectTo(response, () -> provider.callbackWithInstitution(user, timestamp, auth, institution)));
	}
}
