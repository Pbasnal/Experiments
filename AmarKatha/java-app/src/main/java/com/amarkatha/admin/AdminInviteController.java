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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminInviteController {

    private final InviteService inviteService;
    private final AdminDashboardService adminDashboardService;

    public AdminInviteController(InviteService inviteService, AdminDashboardService adminDashboardService) {
        this.inviteService = inviteService;
        this.adminDashboardService = adminDashboardService;
    }

    @GetMapping({"", "/"})
    public String adminHome(Model model, @AuthenticationPrincipal AmarKathaPrincipal principal) {
        model.addAttribute("navActive", "dashboard");
        model.addAttribute("adminEmail", principal.getUser().getEmail());
        model.addAttribute("dashboard", adminDashboardService.dashboard());
        return "admin/dashboard";
    }

    @GetMapping("/invites")
    public String listInvites(
            @RequestParam(value = "status", required = false, defaultValue = "all") String status,
            Model model,
            @AuthenticationPrincipal AmarKathaPrincipal principal
    ) {
        model.addAttribute("navActive", "invites");
        model.addAttribute("adminEmail", principal.getUser().getEmail());
        model.addAttribute("statusFilter", status);
        model.addAttribute("invites", adminDashboardService.listInviteRows(status));
        return "admin/invites";
    }

    @PostMapping("/invites")
    public String generateInvite(
            @AuthenticationPrincipal AmarKathaPrincipal principal,
            @RequestParam(value = "redirect", required = false) String redirect,
            RedirectAttributes redirectAttributes
    ) {
        InviteToken invite = inviteService.generate(principal.getUser());
        redirectAttributes.addFlashAttribute("newToken", invite.getToken());
        if ("dashboard".equals(redirect)) {
            return "redirect:/admin";
        }
        return "redirect:/admin/invites";
    }
}
