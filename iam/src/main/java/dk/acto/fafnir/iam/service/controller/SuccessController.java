package dk.acto.fafnir.iam.service.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping("iam/success")
public class SuccessController {

    @GetMapping
    public ModelAndView success() {
        return new ModelAndView("success");
    }

}
