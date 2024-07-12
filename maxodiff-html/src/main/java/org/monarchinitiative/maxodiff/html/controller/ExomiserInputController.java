package org.monarchinitiative.maxodiff.html.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/exomiserInput")
public class ExomiserInputController {

    @RequestMapping
    public String getInput() {
        return "exomiserInput";
    }
}
