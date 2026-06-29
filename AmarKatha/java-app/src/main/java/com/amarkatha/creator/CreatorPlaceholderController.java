package com.amarkatha.creator;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/creator")
public class CreatorPlaceholderController {

    @GetMapping({"", "/"})
    public String creatorHome(Model model) {
        model.addAttribute("title", "Creator Portal");
        model.addAttribute("description", "Upload chapters, set your schedule, skip a week without guilt, and copy share links.");
        return "creator/placeholder";
    }

    @GetMapping("/series")
    public String seriesList(Model model) {
        model.addAttribute("title", "My Series");
        model.addAttribute("description", "Manage up to 5 ongoing series. Set weekly or biweekly cadence after your second chapter.");
        return "creator/placeholder";
    }

    @GetMapping("/onboard")
    public String onboard(Model model) {
        model.addAttribute("title", "Become a Creator");
        model.addAttribute("description", "Sign in with Google and redeem your invite token to start publishing.");
        return "creator/placeholder";
    }
}
