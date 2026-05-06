package org.example.util;

import java.nio.file.Path;
import java.util.regex.Pattern;

public final class SafePath {

    private static final Pattern SEGMENT = Pattern.compile("^[a-zA-Z0-9][a-zA-Z0-9._-]{0,255}$");

    private SafePath() {}

    public static boolean isSafeSegment(String value) {
        return value != null && SEGMENT.matcher(value).matches() && !value.contains("..");
    }

    public static Path child(Path root, String seriesId) {
        if (!isSafeSegment(seriesId)) {
            throw new IllegalArgumentException("Invalid series id");
        }
        Path resolved = root.resolve(seriesId).normalize();
        if (!resolved.startsWith(root.normalize())) {
            throw new IllegalArgumentException("Path escapes root");
        }
        return resolved;
    }

    public static Path chapterDir(Path seriesPath, String chapterId) {
        if (!isSafeSegment(chapterId)) {
            throw new IllegalArgumentException("Invalid chapter id");
        }
        Path resolved = seriesPath.resolve(chapterId).normalize();
        if (!resolved.startsWith(seriesPath.normalize())) {
            throw new IllegalArgumentException("Path escapes series");
        }
        return resolved;
    }
}
