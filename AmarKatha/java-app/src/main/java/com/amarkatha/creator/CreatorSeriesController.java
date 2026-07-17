package com.amarkatha.creator;

import com.amarkatha.identity.security.AmarKathaPrincipal;
import com.amarkatha.publishing.ChapterService;
import com.amarkatha.publishing.OngoingSeriesCapExceededException;
import com.amarkatha.publishing.ScheduleServiceException;
import com.amarkatha.publishing.SeriesAccessException;
import com.amarkatha.publishing.SeriesScheduleService;
import com.amarkatha.publishing.SeriesService;
import com.amarkatha.publishing.domain.Series;
import com.amarkatha.scheduling.ScheduleCalendar;
import com.amarkatha.scheduling.ScheduleStripView;
import java.time.DateTimeException;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
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

    private static final DateTimeFormatter NEXT_EXPECTED_FMT =
            DateTimeFormatter.ofPattern("EEE, d MMM yyyy 'at' h a").withLocale(Locale.ENGLISH);

    private final SeriesService seriesService;
    private final ChapterService chapterService;
    private final SeriesScheduleService seriesScheduleService;

    public CreatorSeriesController(
            SeriesService seriesService,
            ChapterService chapterService,
            SeriesScheduleService seriesScheduleService
    ) {
        this.seriesService = seriesService;
        this.chapterService = chapterService;
        this.seriesScheduleService = seriesScheduleService;
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
        ScheduleStripView strip = seriesScheduleService.stripFor(series);
        model.addAttribute("user", principal);
        model.addAttribute("series", series);
        model.addAttribute("chapters", chapterService.listForSeries(id, principal.getId()));
        model.addAttribute("scheduleStrip", strip);
        model.addAttribute("promptCadence", seriesScheduleService.shouldPromptCadence(id, principal.getId()));
        model.addAttribute("nextExpectedLabel", formatNextExpected(series));
        model.addAttribute("defaultReleaseHour", ScheduleCalendar.DEFAULT_RELEASE_HOUR_IST);
        String[] hourLabels = new String[24];
        for (int h = 0; h < 24; h++) {
            int displayHour = h % 12 == 0 ? 12 : h % 12;
            String amPm = h < 12 ? "AM" : "PM";
            hourLabels[h] = String.format("%d:00 %s", displayHour, amPm);
        }
        model.addAttribute("hourLabels", hourLabels);
        model.addAttribute("dayNames", new String[]{
                "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"
        });
        return "creator/series-detail";
    }

    @PostMapping("/{id}/schedule")
    public String activateSchedule(
            @PathVariable UUID id,
            @AuthenticationPrincipal AmarKathaPrincipal principal,
            @RequestParam int periodDays,
            @RequestParam int dayOfWeek,
            @RequestParam int releaseHourIst,
            RedirectAttributes redirectAttributes
    ) {
        try {
            seriesScheduleService.activateCadence(
                    id,
                    principal.getId(),
                    periodDays,
                    dayOfWeek,
                    releaseHourIst
            );
            redirectAttributes.addFlashAttribute("success", "Schedule updated.");
        } catch (ScheduleServiceException | SeriesAccessException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/creator/series/" + id;
    }

    @PostMapping("/{id}/schedule/clear")
    public String clearSchedule(
            @PathVariable UUID id,
            @AuthenticationPrincipal AmarKathaPrincipal principal,
            RedirectAttributes redirectAttributes
    ) {
        try {
            seriesScheduleService.clearCadence(id, principal.getId());
            redirectAttributes.addFlashAttribute("success", "Schedule turned off.");
        } catch (ScheduleServiceException | SeriesAccessException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/creator/series/" + id;
    }

    @PostMapping("/{id}/schedule/skip")
    public String skip(
            @PathVariable UUID id,
            @AuthenticationPrincipal AmarKathaPrincipal principal,
            @RequestParam(required = false) String skipMessage,
            RedirectAttributes redirectAttributes
    ) {
        try {
            seriesScheduleService.skip(id, principal.getId(), skipMessage);
            redirectAttributes.addFlashAttribute("success", "Skipped next slot.");
        } catch (ScheduleServiceException | SeriesAccessException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/creator/series/" + id;
    }

    @PostMapping("/{id}/schedule/hiatus")
    public String hiatus(
            @PathVariable UUID id,
            @AuthenticationPrincipal AmarKathaPrincipal principal,
            RedirectAttributes redirectAttributes
    ) {
        try {
            seriesScheduleService.hiatus(id, principal.getId());
            redirectAttributes.addFlashAttribute("success", "Series is on hiatus.");
        } catch (ScheduleServiceException | SeriesAccessException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/creator/series/" + id;
    }

    @PostMapping("/{id}/schedule/resume")
    public String resume(
            @PathVariable UUID id,
            @AuthenticationPrincipal AmarKathaPrincipal principal,
            RedirectAttributes redirectAttributes
    ) {
        try {
            seriesScheduleService.resume(id, principal.getId());
            redirectAttributes.addFlashAttribute("success", "Series resumed.");
        } catch (ScheduleServiceException | SeriesAccessException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/creator/series/" + id;
    }

    private static String formatNextExpected(Series series) {
        if (series.getNextExpectedAt() == null) {
            return null;
        }
        try {
            return NEXT_EXPECTED_FMT.format(series.getNextExpectedAt().atZone(ScheduleCalendar.IST))
                    + " (IST)";
        } catch (DateTimeException ex) {
            return series.getNextExpectedAt().toString();
        }
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
