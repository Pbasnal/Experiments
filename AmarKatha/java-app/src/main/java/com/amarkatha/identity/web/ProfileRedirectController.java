package com.amarkatha.identity.web;

import com.amarkatha.identity.security.AmarKathaPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ProfileRedirectController {

    @GetMapping("/profile")
    public String profile(@AuthenticationPrincipal AmarKathaPrincipal principal) {
        if (principal == null) {
            return "redirect:/login";
        }
        return switch (principal.getRole()) {
            case ADMIN -> "redirect:/admin/profile";
            case CREATOR -> "redirect:/creator/profile";
            case READER -> "redirect:/read/profile";
        };
    }
}
