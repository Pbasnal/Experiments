package org.example.api.dto;

import java.util.List;

public record SeriesListResponse(List<SeriesSummaryDto> items, long total, int page, int pageSize) {}
