package com.amarkatha.admin;

import com.amarkatha.identity.InviteService;
import com.amarkatha.identity.UserRepository;
import com.amarkatha.identity.domain.InviteToken;
import com.amarkatha.identity.domain.User;
import com.amarkatha.identity.security.AmarKathaPrincipal;
import com.amarkatha.shared.demo.DemoPreviewState;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminInviteController {

    private final InviteService inviteService;
    private final AdminDashboardService adminDashboardService;
    private final UserRepository userRepository;

    public AdminInviteController(
            InviteService inviteService,
            AdminDashboardService adminDashboardService,
            UserRepository userRepository
    ) {
        this.inviteService = inviteService;
        this.adminDashboardService = adminDashboardService;
        this.userRepository = userRepository;
    }

    @GetMapping({"", "/"})
    public String adminHome(Model model, @AuthenticationPrincipal AmarKathaPrincipal principal) {
        model.addAttribute("navActive", "dashboard");
        model.addAttribute("adminEmail", principal.getEmail());
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
        model.addAttribute("adminEmail", principal.getEmail());
        model.addAttribute("statusFilter", status);
        model.addAttribute("invites", adminDashboardService.listInviteRows(status));
        return "admin/invites";
    }

    @GetMapping("/profile")
    public String profile(Model model, @AuthenticationPrincipal AmarKathaPrincipal principal) {
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));
        model.addAttribute("navActive", "profile");
        model.addAttribute("adminEmail", principal.getEmail());
        model.addAttribute("displayName", principal.getDisplayName());
        model.addAttribute("role", principal.getRole().name());
        model.addAttribute("demoMode", user.isDemoMode());
        return "admin/profile";
    }

    @PostMapping("/profile/demo")
    public String updateDemoMode(
            @AuthenticationPrincipal AmarKathaPrincipal principal,
            @RequestParam(value = "demoMode", defaultValue = "false") boolean demoMode,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));
        user.setDemoMode(demoMode);
        userRepository.save(user);
        if (!demoMode) {
            DemoPreviewState.clear(session);
        }
        redirectAttributes.addFlashAttribute(
                "success",
                demoMode ? "Demo preview is on." : "Demo preview is off. Live data is showing."
        );
        return "redirect:/admin/profile";
    }

    @PostMapping("/profile/demo/reset")
    public String resetDemoPreview(HttpSession session, RedirectAttributes redirectAttributes) {
        DemoPreviewState.clear(session);
        redirectAttributes.addFlashAttribute("success", "Demo preview reset to the sample library.");
        return "redirect:/admin/profile";
    }

    @PostMapping("/invites")
    public String generateInvite(
            @AuthenticationPrincipal AmarKathaPrincipal principal,
            @RequestParam(value = "redirect", required = false) String redirect,
            @RequestParam(value = "maxUses", required = false, defaultValue = "1") int maxUses,
            RedirectAttributes redirectAttributes
    ) {
        if (maxUses < InviteService.minMaxUses() || maxUses > InviteService.maxMaxUses()) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "Max uses must be between " + InviteService.minMaxUses()
                            + " and " + InviteService.maxMaxUses() + "."
            );
            if ("dashboard".equals(redirect)) {
                return "redirect:/admin";
            }
            return "redirect:/admin/invites";
        }
        User admin = userRepository.getReferenceById(principal.getId());
        InviteToken invite = inviteService.generate(admin, maxUses);
        redirectAttributes.addFlashAttribute("newToken", invite.getToken());
        redirectAttributes.addFlashAttribute("newMaxUses", invite.getMaxUses());
        if ("dashboard".equals(redirect)) {
            return "redirect:/admin";
        }
        return "redirect:/admin/invites";
    }
}
