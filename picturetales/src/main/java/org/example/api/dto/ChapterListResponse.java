package org.example.api.dto;

import java.util.List;

public record ChapterListResponse(String seriesId, List<ChapterDto> chapters) {}
