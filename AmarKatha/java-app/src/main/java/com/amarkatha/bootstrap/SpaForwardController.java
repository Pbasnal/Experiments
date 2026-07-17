package com.amarkatha.bootstrap;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaForwardController {

    @GetMapping(value = {
            "/",
            "/read",
            "/read/profile",
            "/profile"
    })
    public String forwardReaderSpa() {
        return "forward:/index.html";
    }
}
