package dk.acto.fafnir.iam.service.controller;

import dk.acto.fafnir.api.exception.UserUpdateFailed;
import dk.acto.fafnir.api.model.Slice;
import dk.acto.fafnir.api.model.UserData;
import dk.acto.fafnir.api.service.AdministrationService;
import dk.acto.fafnir.iam.dto.DtoFactory;
import lombok.AllArgsConstructor;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import java.time.Instant;
import java.util.Map;

@Controller
@AllArgsConstructor
@RequestMapping("iam/usr")
@PreAuthorize("hasAuthority(T(dk.acto.fafnir.iam.security.IAMRoles).FAFNIR_ADMIN.toString())")
public class UserController {
    private final AdministrationService administrationService;
    private final DtoFactory dtoFactory;

    @GetMapping("page/{pageNumber}")
    public ModelAndView getUserOverview(@PathVariable Long pageNumber) {
        var maxValue = administrationService.countOrganisations();
        var pageActual = Slice.cropPage(pageNumber, maxValue);
        if (!pageActual.equals(pageNumber - 1)) {
            return new ModelAndView("redirect:/iam/usr/page/" + (pageActual + 1));
        }
        var result = administrationService.readUsers(pageActual);
        var model = dtoFactory.calculatePageData(pageActual, maxValue, "/iam/org");
        model.put("tableData", result.getPageData());
        return new ModelAndView("user_overview", model);
    }

    @GetMapping("{subject}")
    public ModelAndView getUserDetail(@PathVariable String subject) {
        var result = administrationService.readUser(subject);
        var model = Map.of(
                "tableData", result,
                "isNew", false
        );
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
                .created(Instant.now())
                .build();
        var model = Map.of(
                "tableData", result,
                "isNew", true
        );
        return new ModelAndView("user_detail", model);
    }


    @PostMapping("{subject}")
    public RedirectView updateUser(@ModelAttribute UserData user, @PathVariable String subject) {
        if (!subject.equals(user.getSubject())) {
            throw new UserUpdateFailed();
        }
        administrationService.updateUser(user);
        return new RedirectView("/iam/usr/page/1");
    }

    @PostMapping
    public RedirectView createUser(@ModelAttribute UserData usr) {
        administrationService.createUser(usr);
        return new RedirectView("/iam/usr/page/1");
    }
}
