package dk.acto.fafnir.iam.service.controller;

import dk.acto.fafnir.api.model.OrganisationData;
import dk.acto.fafnir.api.model.UserData;
import dk.acto.fafnir.api.service.AdministrationService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;

@Controller
@Slf4j
@AllArgsConstructor
@RequestMapping("iam/usr")
public class UserController {
    private final AdministrationService administrationService;

    @GetMapping("page/{pageNumber}")
    public ModelAndView getUserOverview(@PathVariable Long pageNumber) {
        var result = administrationService.readUsers(pageNumber);
        var model = Map.of("page", pageNumber,
                "pages", result.getTotalPages(),
                "tableData", result.getPageData());
        return new ModelAndView("user_overview", model);
    }

    @GetMapping("{subject}")
    public ModelAndView getUserDetail(@PathVariable String subject) {
        var result = administrationService.readUser(subject);
        var model = Map.of("tableData", result);
        return new ModelAndView("user_detail", model);
    }

    @GetMapping
    public ModelAndView getEmptyUserDetail() {
        var result = UserData.builder()
                .created(Instant.now())
                .locale(LocaleContextHolder.getLocale())
                .subject("")
                .metaId("")
                .name("")
                .password("")
                .build();
        var model = Map.of("tableData", result);
        return new ModelAndView("user_detail", model);
    }

    @PostMapping
    public RedirectView createUser(@ModelAttribute UserData usr) {
        administrationService.createUser(usr);
        return new RedirectView("/iam/usr/page/0");
    }

}
