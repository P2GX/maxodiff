package org.monarchinitiative.maxodiff.html.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/nullInput")
public class NullInputController {

    @RequestMapping
    public String getInput() {
        return "nullInput";
    }

    @RequestMapping("/engineInput")
    public String engineInput() {
        return "engineInput";
    }
}
