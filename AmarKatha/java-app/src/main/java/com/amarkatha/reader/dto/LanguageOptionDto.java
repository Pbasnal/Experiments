package com.amarkatha.reader.dto;

public record LanguageOptionDto(
        String code,
        String label,
        String nativeLabel,
        int seriesCount
) {
}
