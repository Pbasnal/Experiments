package com.amarkatha.creator;

import com.amarkatha.identity.security.AmarKathaPrincipal;
import com.amarkatha.publishing.ChapterRepository;
import com.amarkatha.publishing.SeriesCoverPresentation;
import com.amarkatha.publishing.SeriesScheduleService;
import com.amarkatha.publishing.SeriesService;
import com.amarkatha.publishing.domain.Series;
import com.amarkatha.scheduling.ScheduleCalendar;
import com.amarkatha.scheduling.ScheduleStripView;
import com.amarkatha.shared.domain.ChapterState;
import com.amarkatha.shared.domain.SeriesStatus;
import java.time.DateTimeException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/creator")
public class CreatorController {

    private static final DateTimeFormatter NEXT_SLOT_FMT =
            DateTimeFormatter.ofPattern("EEE, d MMM yyyy 'at' h a").withLocale(Locale.ENGLISH);

    private final SeriesService seriesService;
    private final SeriesScheduleService seriesScheduleService;
    private final ChapterRepository chapterRepository;

    public CreatorController(
            SeriesService seriesService,
            SeriesScheduleService seriesScheduleService,
            ChapterRepository chapterRepository
    ) {
        this.seriesService = seriesService;
        this.seriesScheduleService = seriesScheduleService;
        this.chapterRepository = chapterRepository;
    }

    @GetMapping({"", "/"})
    public String creatorHome(@AuthenticationPrincipal AmarKathaPrincipal principal, Model model) {
        UUID creatorId = principal.getId();
        List<Series> seriesList = seriesService.listForCreator(creatorId);
        List<CreatorHomeSeriesView> homeSeries = new ArrayList<>();
        int totalDrafts = 0;
        int needsScheduleCount = 0;

        for (Series series : seriesList) {
            long drafts = chapterRepository.countBySeriesIdAndState(series.getId(), ChapterState.DRAFT);
            totalDrafts += (int) drafts;
            boolean prompt = seriesScheduleService.shouldPromptCadence(series.getId(), creatorId);
            if (prompt) {
                needsScheduleCount++;
            }
            ScheduleStripView strip = seriesScheduleService.stripFor(series);
            homeSeries.add(toRow(series, drafts, prompt, strip.scheduleLabel(), formatNextSlot(series)));
        }

        model.addAttribute("user", principal);
        model.addAttribute("seriesList", homeSeries);
        model.addAttribute("ongoingCount", seriesService.countOngoing(creatorId));
        model.addAttribute("maxOngoing", SeriesService.MAX_ONGOING_SERIES_PER_CREATOR);
        model.addAttribute("canCreate", seriesService.canCreateOngoing(creatorId));
        model.addAttribute("totalDrafts", totalDrafts);
        model.addAttribute("needsScheduleCount", needsScheduleCount);
        return "creator/home";
    }

    @GetMapping("/profile")
    public String profile(@AuthenticationPrincipal AmarKathaPrincipal principal, Model model) {
        model.addAttribute("user", principal);
        return "creator/profile";
    }

    static CreatorHomeSeriesView toRow(
            Series series,
            long drafts,
            boolean needsSchedulePrompt,
            String scheduleSummary,
            String nextSlotLabel
    ) {
        return new CreatorHomeSeriesView(
                series.getId(),
                series.getTitle(),
                series.getSlug(),
                series.getStatus().name(),
                series.getContentLanguage(),
                SeriesCoverPresentation.coverUrl(series.getCoverStorageKey(), series.getVersion()),
                SeriesCoverPresentation.coverGradient(series.getSlug()),
                series.getDescription(),
                nextSlotLabel,
                scheduleSummary,
                drafts,
                needsSchedulePrompt,
                series.getStatus() == SeriesStatus.HIATUS
        );
    }

    private static String formatNextSlot(Series series) {
        if (series.getStatus() == SeriesStatus.HIATUS) {
            return "On hiatus";
        }
        if (series.getStatus() == SeriesStatus.COMPLETED) {
            return "Completed";
        }
        if (series.getNextExpectedAt() == null) {
            return "No schedule set";
        }
        try {
            return "Next: "
                    + NEXT_SLOT_FMT.format(series.getNextExpectedAt().atZone(ScheduleCalendar.IST))
                    + " IST";
        } catch (DateTimeException ex) {
            return "Next: " + series.getNextExpectedAt();
        }
    }
}
