package com.amarkatha.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminPlaceholderController {

    @GetMapping({"", "/"})
    public String adminHome(Model model) {
        model.addAttribute("title", "Admin Portal");
        model.addAttribute("description", "Generate invite tokens, track creator stipends, and handle content reports.");
        return "admin/placeholder";
    }

    @GetMapping("/invites")
    public String invites(Model model) {
        model.addAttribute("title", "Invite Tokens");
        model.addAttribute("description", "Create one-time invite links for the creator cohort.");
        return "admin/placeholder";
    }
}
