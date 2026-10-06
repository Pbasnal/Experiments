package com.amarkatha.shared.demo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Deterministic sample catalog used only while an admin has demo mode on.
 */
public final class DemoLibrary {

    public static final String PAGE_ONE = "/demo/page-1.svg";
    public static final String PAGE_TWO = "/demo/page-2.svg";
    public static final String PAGE_THREE = "/demo/page-3.svg";
    public static final String PAGE_FOUR = "/demo/page-4.svg";

    public static final UUID NOTICE_MONSOON = UUID.fromString("6f1c0b10-7a11-4c2e-9a10-000000000001");
    public static final UUID NOTICE_PLATFORM = UUID.fromString("6f1c0b10-7a11-4c2e-9a10-000000000002");
    public static final UUID NOTICE_PAPER = UUID.fromString("6f1c0b10-7a11-4c2e-9a10-000000000003");
    public static final UUID NOTICE_SALT = UUID.fromString("6f1c0b10-7a11-4c2e-9a10-000000000004");
    public static final UUID NOTICE_GLIMPSE = UUID.fromString("6f1c0b10-7a11-4c2e-9a10-000000000005");

    public static final UUID GLIMPSE_MONSOON_CHARACTER = UUID.fromString("6f1c0b10-7a11-4c2e-9a20-000000000001");
    public static final UUID GLIMPSE_MONSOON_CHAR_1 = UUID.fromString("6f1c0b10-7a11-4c2e-9a20-000000000002");
    public static final UUID GLIMPSE_MONSOON_CHAR_2 = UUID.fromString("6f1c0b10-7a11-4c2e-9a20-000000000003");
    public static final UUID GLIMPSE_MONSOON_CHAR_3 = UUID.fromString("6f1c0b10-7a11-4c2e-9a20-000000000004");
    public static final UUID GLIMPSE_MONSOON_TEASER = UUID.fromString("6f1c0b10-7a11-4c2e-9a20-000000000011");
    public static final UUID GLIMPSE_MONSOON_TEASE_1 = UUID.fromString("6f1c0b10-7a11-4c2e-9a20-000000000012");
    public static final UUID GLIMPSE_MONSOON_TEASE_2 = UUID.fromString("6f1c0b10-7a11-4c2e-9a20-000000000013");
    public static final UUID GLIMPSE_SALT_LORE = UUID.fromString("6f1c0b10-7a11-4c2e-9a20-000000000021");
    public static final UUID GLIMPSE_SALT_LORE_1 = UUID.fromString("6f1c0b10-7a11-4c2e-9a20-000000000022");

    private static final List<DemoStory> STORIES = List.of(
            story("monsoon-market", "Monsoon Market", "Meera Iyer",
                    "A night market that only opens when the rain does.",
                    List.of("Slice of life", "City"), "en", "ONGOING", "Every Friday", "Next chapter Friday",
                    7, 4.8, 128_400, true, 0, true, "chapter-2"),
            story("salt-and-saffron", "Salt and Saffron", "Kabir Menon",
                    "A family kitchen, three generations, and one recipe nobody will write down.",
                    List.of("Drama", "Family"), "hi", "ONGOING", "Every Sunday", "Next chapter Sunday",
                    7, 4.6, 86_400, true, 1, true, "chapter-3"),
            story("platform-three", "Platform Three", "Asha Raman",
                    "The last local train out of a coastal town, and the people who miss it on purpose.",
                    List.of("Mystery", "Travel"), "ta", "ONGOING", "Every 14 days", "Next chapter in 6 days",
                    14, 4.9, 210_300, true, 2, true, "chapter-1"),
            story("paper-boats", "Paper Boats", "Farhan Qureshi",
                    "Letters folded into boats and sent down a river that remembers.",
                    List.of("Fantasy", "Coming of age"), "bn", "ONGOING", "Every Wednesday", "Next chapter Wednesday",
                    7, 4.5, 27_600, true, 1, true, null),
            story("night-bus", "Night Bus", "Meera Iyer",
                    "A completed road story about the seats people leave empty.",
                    List.of("Drama"), "en", "COMPLETED", "Complete", "Series complete",
                    0, 4.7, 98_000, false, 12, false, "chapter-4"),
            story("ink-in-the-rain", "Ink in the Rain", "Leela Das",
                    "A muralist paints over the city before the monsoon can.",
                    List.of("Art", "City"), "hi", "ONGOING", "Every 10 days", "Next chapter in 4 days",
                    10, 4.4, 33_100, false, 3, false, null),
            story("courtyard", "Courtyard", "Asha Raman",
                    "Four apartments, one shared courtyard, and a festival that will not wait.",
                    List.of("Slice of life"), "ta", "ONGOING", "Every Saturday", "Next chapter Saturday",
                    7, 4.1, 12_800, false, 4, false, null),
            story("river-post", "River Post", "Farhan Qureshi",
                    "On hiatus while the creator finishes the next arc.",
                    List.of("Adventure"), "bn", "HIATUS", "On hiatus", "Paused",
                    0, 4.2, 45_200, false, 20, false, "chapter-2"),
            story("last-bell", "Last Bell", "Kabir Menon",
                    "The final term at a school that is about to be sold.",
                    List.of("Drama", "School"), "en", "ONGOING", "Every Monday", "Next chapter Monday",
                    7, 3.9, 8_400, false, 5, false, null),
            story("mango-season", "Mango Season", "Leela Das",
                    "A completed summer about cousins, a radio, and one perfect mango.",
                    List.of("Slice of life"), "hi", "COMPLETED", "Complete", "Series complete",
                    0, 4.8, 156_000, false, 9, false, "chapter-4"),
            story("signal-hill", "Signal Hill", "Meera Iyer",
                    "Messages passed by lamp from a hill above the harbour.",
                    List.of("Historical", "Adventure"), "ta", "ONGOING", "Every 14 days", "Next chapter in 9 days",
                    14, 4.3, 19_300, false, 6, false, null),
            story("winter-letters", "Winter Letters", "Kabir Menon",
                    "A postal clerk in the hills starts answering the letters nobody claims.",
                    List.of("Drama"), "bn", "ONGOING", "Every Thursday", "Next chapter Thursday",
                    7, 4.0, 15_700, false, 2, false, null)
    );

    private static final List<DemoNotice> NOTICES = List.of(
            new DemoNotice(NOTICE_MONSOON, "monsoon-market", "chapter-4",
                    "New chapter: Monsoon Market", "Chapter 4 is ready to read.", 2, true, null),
            new DemoNotice(NOTICE_GLIMPSE, "monsoon-market", null,
                    "New glimpse: Monsoon Market", "Meera shared a character turnaround.", 6, true,
                    GLIMPSE_MONSOON_CHARACTER),
            new DemoNotice(NOTICE_PLATFORM, "platform-three", "chapter-4",
                    "New chapter: Platform Three", "Asha Raman published Chapter 4.", 20, true, null),
            new DemoNotice(NOTICE_PAPER, "paper-boats", "chapter-3",
                    "New chapter: Paper Boats", "Chapter 3 went out on Wednesday.", 50, false, null),
            new DemoNotice(NOTICE_SALT, "salt-and-saffron", "chapter-4",
                    "New chapter: Salt and Saffron", "The Sunday chapter is up.", 80, false, null)
    );

    private static final List<DemoGlimpse> GLIMPSES = List.of(
            new DemoGlimpse(GLIMPSE_MONSOON_CHARACTER, "monsoon-market", "CHARACTER", 18, List.of(
                    new DemoGlimpseImage(GLIMPSE_MONSOON_CHAR_1, "/demo/glimpses/monsoon-character-1.svg", 1, 18),
                    new DemoGlimpseImage(GLIMPSE_MONSOON_CHAR_2, "/demo/glimpses/monsoon-character-2.svg", 2, 7),
                    new DemoGlimpseImage(GLIMPSE_MONSOON_CHAR_3, "/demo/glimpses/monsoon-character-3.svg", 3, 3)
            )),
            new DemoGlimpse(GLIMPSE_MONSOON_TEASER, "monsoon-market", "TEASER", 10, List.of(
                    new DemoGlimpseImage(GLIMPSE_MONSOON_TEASE_1, "/demo/glimpses/monsoon-teaser-1.svg", 1, 4),
                    new DemoGlimpseImage(GLIMPSE_MONSOON_TEASE_2, "/demo/glimpses/monsoon-teaser-2.svg", 2, 1)
            )),
            new DemoGlimpse(GLIMPSE_SALT_LORE, "salt-and-saffron", "LORE", 4, List.of(
                    new DemoGlimpseImage(GLIMPSE_SALT_LORE_1, "/demo/glimpses/salt-lore.svg", 1, 2)
            ))
    );

    private DemoLibrary() {
    }

    public static List<DemoStory> stories() {
        return STORIES;
    }

    public static Optional<DemoStory> find(String slug) {
        if (slug == null) {
            return Optional.empty();
        }
        return STORIES.stream().filter(story -> story.slug().equalsIgnoreCase(slug.trim())).findFirst();
    }

    public static List<DemoNotice> notices() {
        return NOTICES;
    }

    public static Optional<DemoNotice> notice(UUID id) {
        return NOTICES.stream().filter(notice -> notice.id().equals(id)).findFirst();
    }

    public static List<DemoGlimpse> glimpsesFor(String slug) {
        if (slug == null) {
            return List.of();
        }
        return GLIMPSES.stream().filter(glimpse -> glimpse.seriesSlug().equalsIgnoreCase(slug.trim())).toList();
    }

    public static Optional<DemoGlimpseImage> glimpseImage(UUID glimpseId, UUID imageId) {
        return GLIMPSES.stream()
                .filter(glimpse -> glimpse.id().equals(glimpseId))
                .flatMap(glimpse -> glimpse.images().stream())
                .filter(image -> image.id().equals(imageId))
                .findFirst();
    }

    private static DemoStory story(
            String slug,
            String title,
            String creatorName,
            String description,
            List<String> genres,
            String language,
            String status,
            String scheduleLabel,
            String scheduleHeadline,
            int periodDays,
            double rating,
            int readerCount,
            boolean editorsPick,
            int updatedDaysAgo,
            boolean followedByDefault,
            String defaultProgressChapterSlug
    ) {
        return new DemoStory(
                slug,
                title,
                creatorName,
                description,
                genres,
                language,
                status,
                scheduleLabel,
                scheduleHeadline,
                periodDays,
                rating,
                readerCount,
                editorsPick,
                updatedDaysAgo,
                followedByDefault,
                defaultProgressChapterSlug,
                chapters()
        );
    }

    private static List<DemoChapter> chapters() {
        return List.of(
                new DemoChapter("chapter-1", "Chapter 1", 1, 21),
                new DemoChapter("chapter-2", "Chapter 2", 2, 14),
                new DemoChapter("chapter-3", "Chapter 3", 3, 7),
                new DemoChapter("chapter-4", "Chapter 4", 4, 1)
        );
    }

    public record DemoStory(
            String slug,
            String title,
            String creatorName,
            String description,
            List<String> genres,
            String language,
            String status,
            String scheduleLabel,
            String scheduleHeadline,
            int periodDays,
            double rating,
            int readerCount,
            boolean editorsPick,
            int updatedDaysAgo,
            boolean followedByDefault,
            String defaultProgressChapterSlug,
            List<DemoChapter> chapters
    ) {
        public Optional<DemoChapter> chapter(String chapterSlug) {
            if (chapterSlug == null) {
                return Optional.empty();
            }
            return chapters.stream()
                    .filter(chapter -> chapter.slug().equalsIgnoreCase(chapterSlug.trim()))
                    .findFirst();
        }

        public DemoChapter latestChapter() {
            return chapters.get(chapters.size() - 1);
        }
    }

    public record DemoChapter(String slug, String title, double number, int listedDaysAgo) {
    }

    public record DemoNotice(
            UUID id,
            String seriesSlug,
            String chapterSlug,
            String title,
            String message,
            int ageHours,
            boolean unreadByDefault,
            UUID glimpseId
    ) {
    }

    public record DemoGlimpse(
            UUID id,
            String seriesSlug,
            String tag,
            int postedDaysAgo,
            List<DemoGlimpseImage> images
    ) {
    }

    public record DemoGlimpseImage(UUID id, String url, int sortOrder, long reactionCount) {
    }
}
