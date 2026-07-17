package com.amarkatha.reader;

import com.amarkatha.reader.dto.HomeResponse;
import com.amarkatha.reader.dto.PlatformRouteDto;
import com.amarkatha.reader.dto.SeriesCardDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/reader/v1")
public class HomeApiController {

    private static final Instant NOW = Instant.now();

    @GetMapping("/home")
    public HomeResponse home() {
        List<SeriesCardDto> series = mockSeries().stream()
                .sorted(Comparator.comparing(SeriesCardDto::lastUpdatedAt).reversed())
                .toList();

        return new HomeResponse(
                "Publish on your rhythm, share a link, readers know when you're back.",
                series,
                platformRoutes()
        );
    }

    private static List<SeriesCardDto> mockSeries() {
        return List.of(
                new SeriesCardDto(
                        "monsoon-diaries",
                        "Monsoon Diaries",
                        "Ananya Rao",
                        "Slice-of-life romance set in a Mumbai college during the rains.",
                        List.of("romance", "campus", "slice-of-life"),
                        "en",
                        "linear-gradient(135deg, #6366f1 0%, #a855f7 50%, #ec4899 100%)",
                        "Next update: Friday",
                        "ONGOING",
                        NOW.minus(1, ChronoUnit.DAYS),
                        4
                ),
                new SeriesCardDto(
                        "kalki-rising",
                        "Kalki Rising",
                        "Dev Sharma",
                        "Mythology meets sci-fi as ancient prophecies collide with a near-future India.",
                        List.of("mythology", "drama"),
                        "hi",
                        "linear-gradient(135deg, #f59e0b 0%, #ef4444 55%, #7c3aed 100%)",
                        "On hiatus — back soon",
                        "HIATUS",
                        NOW.minus(5, ChronoUnit.DAYS),
                        7
                ),
                new SeriesCardDto(
                        "hostel-horrors",
                        "Hostel Horrors",
                        "Priya Menon",
                        "Weekly horror anthology from a cursed engineering hostel.",
                        List.of("horror", "campus"),
                        "en",
                        "linear-gradient(135deg, #0f172a 0%, #334155 45%, #dc2626 100%)",
                        "Next update: Sunday",
                        "ONGOING",
                        NOW.minus(2, ChronoUnit.DAYS),
                        12
                ),
                new SeriesCardDto(
                        "chai-and-chaos",
                        "Chai & Chaos",
                        "Rahul Verma",
                        "Two friends run a tea stall that becomes a hub for neighborhood drama.",
                        List.of("slice-of-life", "drama"),
                        "en",
                        "linear-gradient(135deg, #059669 0%, #14b8a6 50%, #6366f1 100%)",
                        "Updates biweekly · Wed",
                        "ONGOING",
                        NOW.minus(3, ChronoUnit.DAYS),
                        3
                ),
                new SeriesCardDto(
                        "saree-superhero",
                        "Saree Superhero",
                        "Meera Iyer",
                        "A Chennai tailor inherits powers stitched into every saree she mends.",
                        List.of("drama", "mythology"),
                        "ta",
                        "linear-gradient(135deg, #db2777 0%, #f97316 60%, #eab308 100%)",
                        "Next update: Saturday",
                        "ONGOING",
                        NOW.minus(6, ChronoUnit.HOURS),
                        5
                ),
                new SeriesCardDto(
                        "midnight-metro",
                        "Midnight Metro",
                        "Arjun Das",
                        "Each last train carries one passenger who never made it home.",
                        List.of("horror", "drama"),
                        "en",
                        "linear-gradient(135deg, #1e1b4b 0%, #4338ca 40%, #06b6d4 100%)",
                        "Completed",
                        "COMPLETED",
                        NOW.minus(14, ChronoUnit.DAYS),
                        8
                )
        );
    }

    private static List<PlatformRouteDto> platformRoutes() {
        return List.of(
                new PlatformRouteDto("Home catalog", "/", "Reader", "preview", "Chronological series feed (V0 — no trending)"),
                new PlatformRouteDto("Series hub", "/read/s/{slug}", "Reader", "preview", "Schedule strip, chapter list, hiatus banner"),
                new PlatformRouteDto("Chapter reader", "/read/s/{slug}/c/{chapter}", "Reader", "planned", "Vertical scroll WebP pages"),
                new PlatformRouteDto("Creator home", "/creator", "Creator", "preview", "Next slot, drafts, quick upload"),
                new PlatformRouteDto("Series management", "/creator/series", "Creator", "planned", "CRUD, schedule, skip/hiatus"),
                new PlatformRouteDto("Chapter editor", "/creator/series/{id}/chapters/{id}/edit", "Creator", "planned", "Multi-image upload & publish"),
                new PlatformRouteDto("Sign up / Sign in", "/creator/signup", "Auth", "preview", "Google OAuth; bootstrap admins skip invite"),
                new PlatformRouteDto("Creator login", "/creator/login", "Auth", "preview", "Returning creators and admins"),
                new PlatformRouteDto("Legal / grievance", "/legal/grievance", "Ops", "planned", "IT Rules compliance pages")
        );
    }
}
