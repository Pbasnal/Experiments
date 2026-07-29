package com.amarkatha.admin;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin")
public class AdminLoginController {

    /**
     * Admin no longer has a separate public login. Bootstrap admins use Google on
     * {@code /creator/signup} or {@code /login}; email must be in ADMIN_BOOTSTRAP_EMAILS.
     */
    @GetMapping("/login")
    public String loginForm(@RequestParam(value = "error", required = false) String error) {
        if (error != null && !error.isBlank()) {
            return "redirect:/login?error=" + error;
        }
        return "redirect:/login";
    }
}
