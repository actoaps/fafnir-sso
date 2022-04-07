package dk.acto.fafnir.iam.service.controller;

import dk.acto.fafnir.api.service.AdministrationService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;

@Controller
@Slf4j
@AllArgsConstructor
@RequestMapping("iam/user")
public class UserController {
    private final AdministrationService administrationService;

    @GetMapping("all/{pageNumber}")
    public ModelAndView getUserOverview(@PathVariable Long pageNumber) {
        var result = administrationService.readUsers(pageNumber);
        var model = Map.of("page", pageNumber,
                "pages", result.getTotalPages(),
                "tableData", result.getPageData());
        return new ModelAndView("user_overview", model);
    }
}
