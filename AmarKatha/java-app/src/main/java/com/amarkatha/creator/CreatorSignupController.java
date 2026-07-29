package com.amarkatha.creator;

import com.amarkatha.identity.InviteInvalidException;
import com.amarkatha.identity.InviteService;
import com.amarkatha.identity.OAuthIntent;
import com.amarkatha.identity.security.AuthSessionKeys;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/creator")
public class CreatorSignupController {

    private final InviteService inviteService;

    public CreatorSignupController(InviteService inviteService) {
        this.inviteService = inviteService;
    }

    @GetMapping("/signup")
    public String signupForm(
            @RequestParam(value = "invite", required = false) String invite,
            @RequestParam(value = "error", required = false) String error,
            Model model
    ) {
        model.addAttribute("invite", invite != null ? invite : "");
        addErrorMessage(error, model);
        return "creator/signup";
    }

    @PostMapping("/signup")
    public String continueWithGoogle(
            @RequestParam(value = "invite", required = false) String invite,
            HttpSession session,
            Model model
    ) {
        if (invite == null || invite.isBlank()) {
            session.setAttribute(AuthSessionKeys.OAUTH_INTENT, OAuthIntent.GOOGLE_AUTH.name());
            session.removeAttribute(AuthSessionKeys.PENDING_INVITE_TOKEN);
            return "redirect:/oauth2/authorization/google";
        }
        try {
            inviteService.validateForSignup(invite);
            session.setAttribute(AuthSessionKeys.OAUTH_INTENT, OAuthIntent.CREATOR_SIGNUP.name());
            session.setAttribute(AuthSessionKeys.PENDING_INVITE_TOKEN, invite.trim());
            return "redirect:/oauth2/authorization/google";
        } catch (InviteInvalidException ex) {
            model.addAttribute("invite", invite);
            model.addAttribute("errorCode", mapInviteError(ex.getReason()));
            return "creator/signup";
        }
    }

    @GetMapping("/login")
    public String loginForm(@RequestParam(value = "error", required = false) String error) {
        if (error != null && !error.isBlank()) {
            return "redirect:/login?error=" + error.trim();
        }
        return "redirect:/login";
    }

    @GetMapping("/onboard")
    public String onboardRedirect(@RequestParam(value = "invite", required = false) String invite) {
        if (invite != null && !invite.isBlank()) {
            return "redirect:/creator/signup?invite=" + invite.trim();
        }
        return "redirect:/creator/signup";
    }

    private static void addErrorMessage(String error, Model model) {
        if (error == null) {
            return;
        }
        model.addAttribute("errorCode", switch (error) {
            case "invite_required" -> "invite_required";
            case "invite_expired" -> "invite_expired";
            case "invite_exhausted", "invite_used" -> "invite_exhausted";
            case "invite_not_found" -> "invite_not_found";
            case "account_not_found" -> "account_not_found";
            case "oauth_failed" -> "oauth_failed";
            case "admin_denied" -> "admin_denied";
            default -> "unknown";
        });
    }

    private static String mapInviteError(InviteInvalidException.InviteInvalidReason reason) {
        return switch (reason) {
            case NOT_FOUND -> "invite_not_found";
            case ALREADY_USED, EXHAUSTED -> "invite_exhausted";
            case EXPIRED -> "invite_expired";
            case BLANK -> "invite_blank";
        };
    }
}
