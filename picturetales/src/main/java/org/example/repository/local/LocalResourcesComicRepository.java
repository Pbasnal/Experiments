package org.example.repository.local;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.example.domain.ChapterInfo;
import org.example.domain.PagedResult;
import org.example.domain.SeriesDetail;
import org.example.domain.SeriesSort;
import org.example.domain.SeriesSummary;
import org.example.repository.ComicCatalogRepository;
import org.example.repository.LocalCatalog;
import org.example.domain.PageAsset;
import org.example.util.SafePath;

@ApplicationScoped
@LocalCatalog
public class LocalResourcesComicRepository implements ComicCatalogRepository {

    private static final List<String> COVER_NAMES =
            List.of("cover.webp", "cover.png", "cover.jpg", "cover.jpeg");

    private static final List<String> IMAGE_EXTENSIONS = List.of(".webp", ".png", ".jpg", ".jpeg", ".gif");

    private final Path root;
    private final ObjectMapper objectMapper;

    @Inject
    public LocalResourcesComicRepository(
            @ConfigProperty(name = "comics.root", defaultValue = "comics-data") String comicsRoot,
            ObjectMapper objectMapper) {
        this.root = Path.of(comicsRoot).toAbsolutePath().normalize();
        this.objectMapper = objectMapper;
    }

    @Override
    public PagedResult<SeriesSummary> listSeries(SeriesSort sort, int page, int pageSize) {
        List<SeriesSummary> all = scanSeriesSummaries(sort);
        return slice(all, page, pageSize);
    }

    @Override
    public PagedResult<SeriesSummary> searchSeries(String query, int page, int pageSize) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        List<SeriesSummary> filtered = scanSeriesSummaries(SeriesSort.POPULAR).stream()
                .filter(s -> q.isEmpty()
                        || s.title().toLowerCase(Locale.ROOT).contains(q)
                        || s.id().toLowerCase(Locale.ROOT).contains(q)
                        || s.descriptionSnippet().toLowerCase(Locale.ROOT).contains(q))
                .toList();
        return slice(filtered, page, pageSize);
    }

    @Override
    public Optional<SeriesDetail> findSeries(String seriesId) {
        try {
            Path seriesPath = SafePath.child(root, seriesId);
            if (!Files.isDirectory(seriesPath)) {
                return Optional.empty();
            }
            SeriesManifest manifest = readSeriesManifest(seriesPath);
            String title = manifest.title != null && !manifest.title.isBlank()
                    ? manifest.title
                    : humanize(seriesId);
            String description = manifest.description != null ? manifest.description : "";
            String status = manifest.status != null ? manifest.status : "UNKNOWN";
            String coverPath = resolveCoverFile(seriesPath).map(p -> "/v1/series/" + seriesId + "/cover").orElse(null);
            return Optional.of(new SeriesDetail(seriesId, title, description, status, coverPath));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    @Override
    public List<ChapterInfo> listChapters(String seriesId) {
        final Path seriesPath;
        try {
            seriesPath = SafePath.child(root, seriesId);
        } catch (IllegalArgumentException ex) {
            return List.of();
        }
        if (!Files.isDirectory(seriesPath)) {
            return List.of();
        }
        List<Path> chapterDirs = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(seriesPath)) {
            for (Path p : stream) {
                if (Files.isDirectory(p) && hasImage(p)) {
                    chapterDirs.add(p);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        chapterDirs.sort(Comparator.comparing(path -> path.getFileName().toString(), naturalOrder()));
        List<ChapterInfo> out = new ArrayList<>();
        for (int i = 0; i < chapterDirs.size(); i++) {
            Path dir = chapterDirs.get(i);
            String chapterId = dir.getFileName().toString();
            String title = readChapterTitle(dir, chapterId);
            out.add(new ChapterInfo(chapterId, title, i));
        }
        return out;
    }

    @Override
    public Optional<Integer> getPageCount(String seriesId, String chapterId) {
        final Path chapterPath;
        try {
            Path seriesPath = SafePath.child(root, seriesId);
            chapterPath = SafePath.chapterDir(seriesPath, chapterId);
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
        if (!Files.isDirectory(chapterPath)) {
            return Optional.empty();
        }
        return Optional.of(listImageFiles(chapterPath).size());
    }

    @Override
    public Optional<PageAsset> openPage(String seriesId, String chapterId, int pageIndex) {
        final Path chapterPath;
        try {
            Path seriesPath = SafePath.child(root, seriesId);
            chapterPath = SafePath.chapterDir(seriesPath, chapterId);
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
        if (!Files.isDirectory(chapterPath)) {
            return Optional.empty();
        }
        List<Path> pages = listImageFiles(chapterPath);
        if (pageIndex < 0 || pageIndex >= pages.size()) {
            return Optional.empty();
        }
        Path file = pages.get(pageIndex);
        try {
            InputStream in = Files.newInputStream(file);
            String type = guessMediaType(file);
            return Optional.of(new PageAsset(type, in));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public Optional<PageAsset> openCover(String seriesId) {
        final Path seriesPath;
        try {
            seriesPath = SafePath.child(root, seriesId);
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
        if (!Files.isDirectory(seriesPath)) {
            return Optional.empty();
        }
        Optional<Path> cover = resolveCoverFile(seriesPath);
        if (cover.isEmpty()) {
            return Optional.empty();
        }
        Path file = cover.get();
        try {
            return Optional.of(new PageAsset(guessMediaType(file), Files.newInputStream(file)));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private PagedResult<SeriesSummary> slice(List<SeriesSummary> all, int page, int pageSize) {
        int p = Math.max(1, page);
        int size = Math.min(100, Math.max(1, pageSize));
        long total = all.size();
        int from = (p - 1) * size;
        if (from >= all.size()) {
            return new PagedResult<>(List.of(), total, p, size);
        }
        int to = Math.min(from + size, all.size());
        return new PagedResult<>(all.subList(from, to), total, p, size);
    }

    private List<SeriesSummary> scanSeriesSummaries(SeriesSort sort) {
        if (!Files.isDirectory(root)) {
            return List.of();
        }
        List<Path> dirs = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(root)) {
            for (Path p : stream) {
                if (Files.isDirectory(p)) {
                    dirs.add(p);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        Comparator<Path> comparator =
                switch (sort) {
                    case LATEST -> Comparator.comparing(this::lastModifiedMillis).reversed();
                    case POPULAR -> Comparator.comparing(path -> path.getFileName().toString(), naturalOrder());
                };
        dirs.sort(comparator);
        List<SeriesSummary> out = new ArrayList<>();
        for (Path dir : dirs) {
            String id = dir.getFileName().toString();
            if (!SafePath.isSafeSegment(id)) {
                continue;
            }
            SeriesManifest manifest = readSeriesManifest(dir);
            String title = manifest.title != null && !manifest.title.isBlank()
                    ? manifest.title
                    : humanize(id);
            String snippet = snippet(manifest.description);
            out.add(new SeriesSummary(id, title, snippet));
        }
        return out;
    }

    private long lastModifiedMillis(Path dir) {
        try {
            return Files.getLastModifiedTime(dir).toMillis();
        } catch (IOException e) {
            return 0L;
        }
    }

    private SeriesManifest readSeriesManifest(Path seriesPath) {
        Path manifestPath = seriesPath.resolve("series.json");
        if (!Files.isRegularFile(manifestPath)) {
            return new SeriesManifest();
        }
        try {
            return objectMapper.readValue(manifestPath.toFile(), SeriesManifest.class);
        } catch (IOException e) {
            return new SeriesManifest();
        }
    }

    private String readChapterTitle(Path chapterDir, String chapterId) {
        Path manifestPath = chapterDir.resolve("chapter.json");
        if (Files.isRegularFile(manifestPath)) {
            try {
                ChapterManifest m = objectMapper.readValue(manifestPath.toFile(), ChapterManifest.class);
                if (m.title != null && !m.title.isBlank()) {
                    return m.title;
                }
            } catch (IOException ignored) {
                // fall through
            }
        }
        return humanize(chapterId);
    }

    private Optional<Path> resolveCoverFile(Path seriesPath) {
        for (String name : COVER_NAMES) {
            Path p = seriesPath.resolve(name);
            if (Files.isRegularFile(p)) {
                return Optional.of(p);
            }
        }
        return Optional.empty();
    }

    private boolean hasImage(Path dir) {
        return !listImageFiles(dir).isEmpty();
    }

    private List<Path> listImageFiles(Path dir) {
        List<Path> files = new ArrayList<>();
        try (Stream<Path> stream = Files.list(dir)) {
            stream.filter(Files::isRegularFile).filter(this::isImageFile).forEach(files::add);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        files.sort(Comparator.comparing(path -> path.getFileName().toString(), naturalOrder()));
        return files;
    }

    private boolean isImageFile(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return IMAGE_EXTENSIONS.stream().anyMatch(name::endsWith);
    }

    private String guessMediaType(Path file) {
        String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
        if (name.endsWith(".png")) {
            return "image/png";
        }
        if (name.endsWith(".webp")) {
            return "image/webp";
        }
        if (name.endsWith(".gif")) {
            return "image/gif";
        }
        return "image/jpeg";
    }

    private static String humanize(String raw) {
        return raw.replace('-', ' ').replace('_', ' ');
    }

    private static String snippet(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String t = text.trim();
        return t.length() > 200 ? t.substring(0, 200) + "…" : t;
    }

    private static Comparator<String> naturalOrder() {
        return Comparator.comparingInt(String::length).thenComparing(String::compareToIgnoreCase);
    }
}
