package com.amarkatha.creator;

import com.amarkatha.identity.security.AmarKathaPrincipal;
import com.amarkatha.publishing.SeriesService;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/creator")
public class CreatorController {

    private final SeriesService seriesService;

    public CreatorController(SeriesService seriesService) {
        this.seriesService = seriesService;
    }

    @GetMapping({"", "/"})
    public String creatorHome(@AuthenticationPrincipal AmarKathaPrincipal principal, Model model) {
        UUID creatorId = principal.getId();
        model.addAttribute("user", principal);
        model.addAttribute("seriesList", seriesService.listForCreator(creatorId));
        model.addAttribute("ongoingCount", seriesService.countOngoing(creatorId));
        model.addAttribute("maxOngoing", SeriesService.MAX_ONGOING_SERIES_PER_CREATOR);
        model.addAttribute("canCreate", seriesService.canCreateOngoing(creatorId));
        return "creator/home";
    }

    @GetMapping("/profile")
    public String profile(@AuthenticationPrincipal AmarKathaPrincipal principal, Model model) {
        model.addAttribute("user", principal);
        return "creator/profile";
    }
}
