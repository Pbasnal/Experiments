package org.example.domain;

import java.util.List;

public record PagedResult<T>(List<T> items, long total, int page, int pageSize) {}
