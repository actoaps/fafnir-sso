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
@RequestMapping("iam/clm")
public class ClaimsController {
    private final AdministrationService administrationService;

    @GetMapping("{orgId}/{subject}")
    public ModelAndView getClaims(@PathVariable String orgId, @PathVariable String subject) {
        var result = administrationService.readClaims(orgId, subject);
        var model = Map.of("tableData", result);
        return new ModelAndView("claims_detail", model);
    }
}
