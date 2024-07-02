package org.monarchinitiative.maxodiff.html.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.SessionAttributes;

@ControllerAdvice
@SessionAttributes("engineName")
class IndexController {

    @ModelAttribute("request")
    String request(HttpServletRequest request,
                      Model model) {
        model.addAttribute("request", request);
        return "";
    }

    @ModelAttribute("requestUri")
    String requestUri(HttpServletRequest request,
                      Model model) {
        model.addAttribute("requestURI", request.getRequestURI());
        return "";
    }

}
