package com.amarkatha.identity.web;

import com.amarkatha.identity.OAuthIntent;
import com.amarkatha.identity.security.AuthSessionKeys;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LoginController {

    @GetMapping("/login")
    public String loginChooser(
            @RequestParam(value = "error", required = false) String error,
            Model model
    ) {
        addErrorMessage(error, model);
        return "login";
    }

    @GetMapping("/login/creator")
    public String signInAsCreator(HttpSession session) {
        session.setAttribute(AuthSessionKeys.OAUTH_INTENT, OAuthIntent.CREATOR_LOGIN.name());
        session.removeAttribute(AuthSessionKeys.PENDING_INVITE_TOKEN);
        return "redirect:/oauth2/authorization/google";
    }

    @GetMapping("/login/reader")
    public String signInAsReader(HttpSession session) {
        session.setAttribute(AuthSessionKeys.OAUTH_INTENT, OAuthIntent.READER_LOGIN.name());
        session.removeAttribute(AuthSessionKeys.PENDING_INVITE_TOKEN);
        return "redirect:/oauth2/authorization/google";
    }

    private static void addErrorMessage(String error, Model model) {
        if (error == null) {
            return;
        }
        model.addAttribute("errorCode", switch (error) {
            case "account_not_found" -> "account_not_found";
            case "oauth_failed" -> "oauth_failed";
            case "admin_denied" -> "admin_denied";
            default -> "unknown";
        });
    }
}
