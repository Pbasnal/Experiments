package com.amarkatha.creator;

import com.amarkatha.identity.security.AmarKathaPrincipal;
import com.amarkatha.publishing.ChapterException;
import com.amarkatha.publishing.ChapterService;
import com.amarkatha.publishing.SeriesAccessException;
import com.amarkatha.publishing.SeriesService;
import com.amarkatha.publishing.domain.Chapter;
import com.amarkatha.publishing.domain.ChapterPage;
import com.amarkatha.publishing.domain.Series;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/creator/series/{seriesId}/chapters")
public class CreatorChapterController {

    private final ChapterService chapterService;
    private final SeriesService seriesService;

    public CreatorChapterController(ChapterService chapterService, SeriesService seriesService) {
        this.chapterService = chapterService;
        this.seriesService = seriesService;
    }

    @PostMapping
    public String createDraft(
            @PathVariable UUID seriesId,
            @AuthenticationPrincipal AmarKathaPrincipal principal,
            @RequestParam(required = false) String title,
            RedirectAttributes redirectAttributes
    ) {
        try {
            Chapter chapter = chapterService.createDraft(seriesId, principal.getId(), title);
            redirectAttributes.addFlashAttribute("success", "Draft chapter created.");
            return "redirect:/creator/series/" + seriesId + "/chapters/" + chapter.getId() + "/edit";
        } catch (SeriesAccessException | ChapterException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/creator/series/" + seriesId;
        }
    }

    @GetMapping("/{chapterId}/edit")
    public String edit(
            @PathVariable UUID seriesId,
            @PathVariable UUID chapterId,
            @AuthenticationPrincipal AmarKathaPrincipal principal,
            Model model
    ) {
        Series series = seriesService.requireOwned(seriesId, principal.getId());
        Chapter chapter = chapterService.requireOwnedChapter(seriesId, chapterId, principal.getId());
        List<ChapterPage> pages = chapterService.listPages(chapterId);
        model.addAttribute("user", principal);
        model.addAttribute("series", series);
        model.addAttribute("chapter", chapter);
        model.addAttribute("pages", pages);
        model.addAttribute("maxPages", chapterService.getMaxPagesPerChapter());
        model.addAttribute("maxPageMb", chapterService.getMaxPageBytes() / (1024 * 1024));
        model.addAttribute("canEdit", chapter.getState().name().equals("DRAFT"));
        return "creator/chapter-editor";
    }

    @PostMapping("/{chapterId}/save")
    public String saveDraft(
            @PathVariable UUID seriesId,
            @PathVariable UUID chapterId,
            @AuthenticationPrincipal AmarKathaPrincipal principal,
            @RequestParam String title,
            RedirectAttributes redirectAttributes
    ) {
        try {
            chapterService.updateDraft(seriesId, chapterId, principal.getId(), title);
            redirectAttributes.addFlashAttribute("success", "Draft saved.");
        } catch (SeriesAccessException | ChapterException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/creator/series/" + seriesId + "/chapters/" + chapterId + "/edit";
    }

    @PostMapping("/{chapterId}/pages")
    public String uploadPages(
            @PathVariable UUID seriesId,
            @PathVariable UUID chapterId,
            @AuthenticationPrincipal AmarKathaPrincipal principal,
            @RequestParam("files") MultipartFile[] files,
            RedirectAttributes redirectAttributes
    ) {
        try {
            List<MultipartFile> list = files == null ? List.of() : Arrays.asList(files);
            chapterService.addPages(seriesId, chapterId, principal.getId(), list);
            redirectAttributes.addFlashAttribute("success", "Pages uploaded.");
        } catch (SeriesAccessException | ChapterException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/creator/series/" + seriesId + "/chapters/" + chapterId + "/edit";
    }

    @PostMapping("/{chapterId}/publish")
    public String publish(
            @PathVariable UUID seriesId,
            @PathVariable UUID chapterId,
            @AuthenticationPrincipal AmarKathaPrincipal principal,
            @RequestParam(value = "copyrightAck", required = false) String copyrightAck,
            RedirectAttributes redirectAttributes
    ) {
        try {
            boolean ack = "true".equalsIgnoreCase(copyrightAck) || "on".equalsIgnoreCase(copyrightAck);
            chapterService.publishNow(seriesId, chapterId, principal.getId(), ack);
            redirectAttributes.addFlashAttribute("success", "Chapter published.");
            return "redirect:/creator/series/" + seriesId;
        } catch (SeriesAccessException | ChapterException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
            return "redirect:/creator/series/" + seriesId + "/chapters/" + chapterId + "/edit";
        }
    }
}
