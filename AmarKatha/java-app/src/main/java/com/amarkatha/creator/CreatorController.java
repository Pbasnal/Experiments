package com.amarkatha.creator;

import com.amarkatha.identity.security.AmarKathaPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/creator")
public class CreatorController {

    @GetMapping({"", "/"})
    public String creatorHome(@AuthenticationPrincipal AmarKathaPrincipal principal, Model model) {
        model.addAttribute("user", principal.getUser());
        return "creator/home";
    }

    @GetMapping("/series")
    public String seriesList(@AuthenticationPrincipal AmarKathaPrincipal principal, Model model) {
        model.addAttribute("user", principal.getUser());
        model.addAttribute("title", "My Series");
        model.addAttribute("description", "Manage up to 5 ongoing series. Set weekly or biweekly cadence after your second chapter.");
        return "creator/placeholder";
    }
}
