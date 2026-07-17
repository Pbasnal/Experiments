package com.amarkatha.creator;

import com.amarkatha.identity.security.AmarKathaPrincipal;
import com.amarkatha.publishing.ChapterService;
import com.amarkatha.publishing.OngoingSeriesCapExceededException;
import com.amarkatha.publishing.SeriesAccessException;
import com.amarkatha.publishing.SeriesService;
import com.amarkatha.publishing.domain.Series;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/creator/series")
public class CreatorSeriesController {

    private final SeriesService seriesService;
    private final ChapterService chapterService;

    public CreatorSeriesController(SeriesService seriesService, ChapterService chapterService) {
        this.seriesService = seriesService;
        this.chapterService = chapterService;
    }

    @GetMapping({"", "/"})
    public String list(@AuthenticationPrincipal AmarKathaPrincipal principal, Model model) {
        UUID creatorId = principal.getId();
        model.addAttribute("user", principal);
        model.addAttribute("seriesList", seriesService.listForCreator(creatorId));
        model.addAttribute("ongoingCount", seriesService.countOngoing(creatorId));
        model.addAttribute("maxOngoing", SeriesService.MAX_ONGOING_SERIES_PER_CREATOR);
        model.addAttribute("canCreate", seriesService.canCreateOngoing(creatorId));
        return "creator/series-list";
    }

    @GetMapping("/new")
    public String newForm(
            @AuthenticationPrincipal AmarKathaPrincipal principal,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (!seriesService.canCreateOngoing(principal.getId())) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "You already have " + SeriesService.MAX_ONGOING_SERIES_PER_CREATOR
                            + " ongoing series. Complete or pause one before creating another."
            );
            return "redirect:/creator/series";
        }
        model.addAttribute("user", principal);
        model.addAttribute("series", null);
        model.addAttribute("formAction", "/creator/series");
        model.addAttribute("pageHeading", "New series");
        return "creator/series-form";
    }

    @PostMapping
    public String create(
            @AuthenticationPrincipal AmarKathaPrincipal principal,
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam(required = false, defaultValue = "en") String contentLanguage,
            RedirectAttributes redirectAttributes
    ) {
        if (title == null || title.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Title is required.");
            return "redirect:/creator/series/new";
        }
        try {
            Series series = seriesService.createSeries(
                    principal.getId(),
                    title,
                    description,
                    contentLanguage
            );
            redirectAttributes.addFlashAttribute("success", "Series created.");
            return "redirect:/creator/series/" + series.getId();
        } catch (OngoingSeriesCapExceededException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/creator/series";
        }
    }

    @GetMapping("/{id}")
    public String detail(
            @PathVariable UUID id,
            @AuthenticationPrincipal AmarKathaPrincipal principal,
            Model model
    ) {
        Series series = seriesService.requireOwned(id, principal.getId());
        model.addAttribute("user", principal);
        model.addAttribute("series", series);
        model.addAttribute("chapters", chapterService.listForSeries(id, principal.getId()));
        return "creator/series-detail";
    }

    @GetMapping("/{id}/edit")
    public String editForm(
            @PathVariable UUID id,
            @AuthenticationPrincipal AmarKathaPrincipal principal,
            Model model
    ) {
        Series series = seriesService.requireOwned(id, principal.getId());
        model.addAttribute("user", principal);
        model.addAttribute("series", series);
        model.addAttribute("formAction", "/creator/series/" + series.getId());
        model.addAttribute("pageHeading", "Edit series");
        return "creator/series-form";
    }

    @PostMapping("/{id}")
    public String update(
            @PathVariable UUID id,
            @AuthenticationPrincipal AmarKathaPrincipal principal,
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam(required = false, defaultValue = "en") String contentLanguage,
            RedirectAttributes redirectAttributes
    ) {
        if (title == null || title.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Title is required.");
            return "redirect:/creator/series/" + id + "/edit";
        }
        try {
            seriesService.updateSeries(id, principal.getId(), title, description, contentLanguage);
            redirectAttributes.addFlashAttribute("success", "Series updated.");
            return "redirect:/creator/series/" + id;
        } catch (SeriesAccessException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/creator/series";
        }
    }
}
