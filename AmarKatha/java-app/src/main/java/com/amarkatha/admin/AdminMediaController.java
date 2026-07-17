package com.amarkatha.admin;

import com.amarkatha.identity.security.AmarKathaPrincipal;
import com.amarkatha.publishing.MediaReconcileService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/media")
@PreAuthorize("hasRole('ADMIN')")
public class AdminMediaController {

    private final MediaReconcileService mediaReconcileService;

    public AdminMediaController(MediaReconcileService mediaReconcileService) {
        this.mediaReconcileService = mediaReconcileService;
    }

    @GetMapping
    public String mediaPage(Model model, @AuthenticationPrincipal AmarKathaPrincipal principal) {
        model.addAttribute("navActive", "media");
        model.addAttribute("adminEmail", principal.getEmail());
        model.addAttribute("brokenListedCount", mediaReconcileService.countBrokenListedChapters());
        return "admin/media";
    }

    @PostMapping("/reconcile")
    public String reconcile(RedirectAttributes redirectAttributes) {
        MediaReconcileService.ReconcileReport report = mediaReconcileService.reconcile();
        redirectAttributes.addFlashAttribute("report", report);
        return "redirect:/admin/media";
    }
}
