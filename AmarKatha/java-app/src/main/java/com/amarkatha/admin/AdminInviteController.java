package com.amarkatha.admin;

import com.amarkatha.identity.InviteService;
import com.amarkatha.identity.domain.InviteToken;
import com.amarkatha.identity.security.AmarKathaPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminInviteController {

    private final InviteService inviteService;

    public AdminInviteController(InviteService inviteService) {
        this.inviteService = inviteService;
    }

    @GetMapping({"", "/"})
    public String adminHome(Model model) {
        model.addAttribute("title", "Admin Portal");
        model.addAttribute("description", "Generate invite tokens, track creator stipends, and handle content reports.");
        return "admin/placeholder";
    }

    @GetMapping("/invites")
    public String listInvites(Model model) {
        model.addAttribute("invites", inviteService.listRecent());
        return "admin/invites";
    }

    @PostMapping("/invites")
    public String generateInvite(
            @AuthenticationPrincipal AmarKathaPrincipal principal,
            RedirectAttributes redirectAttributes
    ) {
        InviteToken invite = inviteService.generate(principal.getUser());
        redirectAttributes.addFlashAttribute("newToken", invite.getToken());
        return "redirect:/admin/invites";
    }
}
