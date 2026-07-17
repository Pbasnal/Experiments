package com.amarkatha.reader;

import com.amarkatha.publishing.domain.Series;
import com.amarkatha.shared.domain.SeriesCadence;
import com.amarkatha.shared.domain.SeriesStatus;
import java.util.List;

final class ReaderPresentation {

    private static final List<String> GRADIENTS = List.of(
            "linear-gradient(135deg, #6366f1 0%, #a855f7 50%, #ec4899 100%)",
            "linear-gradient(135deg, #f59e0b 0%, #ef4444 55%, #7c3aed 100%)",
            "linear-gradient(135deg, #0f172a 0%, #334155 45%, #dc2626 100%)",
            "linear-gradient(135deg, #059669 0%, #14b8a6 50%, #6366f1 100%)",
            "linear-gradient(135deg, #db2777 0%, #f97316 60%, #eab308 100%)",
            "linear-gradient(135deg, #1e1b4b 0%, #4338ca 40%, #06b6d4 100%)"
    );

    private ReaderPresentation() {
    }

    static String coverGradient(String slug) {
        int idx = Math.floorMod(slug == null ? 0 : slug.hashCode(), GRADIENTS.size());
        return GRADIENTS.get(idx);
    }

    static String scheduleLabel(Series series) {
        if (series.getStatus() == SeriesStatus.HIATUS) {
            return "On hiatus — back soon";
        }
        if (series.getStatus() == SeriesStatus.COMPLETED) {
            return "Completed";
        }
        SeriesCadence cadence = series.getCadence();
        return switch (cadence == null ? SeriesCadence.OFF : cadence) {
            case WEEKLY -> "Updates weekly";
            case BIWEEKLY -> "Updates biweekly";
            case OFF -> "Updates when published";
        };
    }

    static String mediaUrl(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            return null;
        }
        return "/media/" + storageKey;
    }
}
