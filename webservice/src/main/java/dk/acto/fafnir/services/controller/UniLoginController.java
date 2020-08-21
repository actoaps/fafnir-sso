package dk.acto.fafnir.services.controller;

import dk.acto.fafnir.FailureReason;
import dk.acto.fafnir.providers.UniLoginProvider;
import dk.acto.fafnir.services.ServiceHelper;
import io.vavr.control.Try;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.Map;

import static dk.acto.fafnir.providers.UniLoginHelper.*;

@Controller
@Slf4j
@RequestMapping("unilogin")
@ConditionalOnBean(UniLoginProvider.class)
@AllArgsConstructor
public class UniLoginController {
	private final UniLoginProvider provider;

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
	public String getOrg(HttpServletResponse response, @RequestParam String user, @RequestParam String timestamp, @RequestParam String auth, @RequestHeader("Accept-Language") String locale, Model model) {
		var institutionList = Try.of(() -> provider.getInstitutionList(user)).getOrElse(Collections.emptyList());
		model.addAllAttributes(
				Map.of(
						USER_ID, user,
						TIMESTAMP, timestamp,
						STATE_AUTH_ENCODED, auth,
						"institutionList", institutionList
				)
		);
		if (institutionList.isEmpty()) {
			Try.of(() -> ServiceHelper.functionalRedirectTo(response, () -> provider.getFailureUrl(FailureReason.CONNECTION_FAILED)));
		}
		return "thymeleaf/ChooseInstitutionUni-Login" + ServiceHelper.getLocaleStr(locale, "da", "en") + ".thymeleaf";
	}

	@PostMapping("org")
	@ResponseBody
	public void postOrg(HttpServletResponse response, @RequestParam String user, @RequestParam String timestamp, @RequestParam String auth, @RequestParam String institution) {
		Try.of(() -> ServiceHelper.functionalRedirectTo(response, () -> provider.callbackWithInstitution(user, timestamp, auth, institution)));
	}
}
