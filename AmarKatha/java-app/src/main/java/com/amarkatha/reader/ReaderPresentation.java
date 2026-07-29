package com.amarkatha.reader;

import com.amarkatha.publishing.SeriesCoverPresentation;

final class ReaderPresentation {

    private ReaderPresentation() {
    }

    static String coverGradient(String slug) {
        return SeriesCoverPresentation.coverGradient(slug);
    }

    static String mediaUrl(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            return null;
        }
        return "/media/" + storageKey;
    }

    /** Cover URLs include a bust token so re-uploads of the same path are not sticky in the browser. */
    static String coverUrl(String storageKey, int version) {
        return SeriesCoverPresentation.coverUrl(storageKey, version);
    }
}
