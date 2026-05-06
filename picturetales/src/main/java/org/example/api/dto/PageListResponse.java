package org.example.api.dto;

import java.util.List;

public record PageListResponse(String seriesId, String chapterId, List<PageRefDto> pages) {}
