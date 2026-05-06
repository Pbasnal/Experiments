package org.example.api;

import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import org.example.api.dto.SeriesListResponse;
import org.example.api.dto.SeriesSummaryDto;
import org.example.domain.PagedResult;
import org.example.domain.SeriesSummary;
import org.example.repository.ComicCatalogRepository;
import org.example.repository.LocalCatalog;

@Path("/v1/search")
@Produces(MediaType.APPLICATION_JSON)
public class V1SearchResource {

    private final ComicCatalogRepository catalog;

    @Inject
    public V1SearchResource(@LocalCatalog ComicCatalogRepository catalog) {
        this.catalog = catalog;
    }

    @GET
    public SeriesListResponse search(
            @QueryParam("q") String q,
            @QueryParam("page") @DefaultValue("1") int page,
            @QueryParam("limit") @DefaultValue("20") int limit) {
        PagedResult<SeriesSummary> result = catalog.searchSeries(q, page, limit);
        return toResponse(result);
    }

    private static SeriesListResponse toResponse(PagedResult<SeriesSummary> result) {
        List<SeriesSummaryDto> items =
                result.items().stream().map(s -> new SeriesSummaryDto(s.id(), s.title(), s.descriptionSnippet())).toList();
        return new SeriesListResponse(items, result.total(), result.page(), result.pageSize());
    }
}
