package org.example.api;

import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.example.api.dto.ChapterDto;
import org.example.api.dto.ChapterListResponse;
import org.example.api.dto.ErrorResponse;
import org.example.api.dto.PageListResponse;
import org.example.api.dto.PageRefDto;
import org.example.api.dto.SeriesDetailDto;
import org.example.api.dto.SeriesListResponse;
import org.example.api.dto.SeriesSummaryDto;
import org.example.domain.ChapterInfo;
import org.example.domain.PagedResult;
import org.example.domain.SeriesDetail;
import org.example.domain.SeriesSort;
import org.example.domain.SeriesSummary;
import org.example.repository.ComicCatalogRepository;
import org.example.repository.LocalCatalog;
import org.example.util.SafePath;

@Path("/v1/series")
@Produces(MediaType.APPLICATION_JSON)
public class V1SeriesResource {

    private final ComicCatalogRepository catalog;

    @Inject
    public V1SeriesResource(@LocalCatalog ComicCatalogRepository catalog) {
        this.catalog = catalog;
    }

    @GET
    public SeriesListResponse list(
            @QueryParam("sort") @DefaultValue("popular") String sortRaw,
            @QueryParam("page") @DefaultValue("1") int page,
            @QueryParam("limit") @DefaultValue("20") int limit) {
        SeriesSort sort = parseSort(sortRaw);
        PagedResult<SeriesSummary> result = catalog.listSeries(sort, page, limit);
        List<SeriesSummaryDto> items =
                result.items().stream().map(s -> new SeriesSummaryDto(s.id(), s.title(), s.descriptionSnippet())).toList();
        return new SeriesListResponse(items, result.total(), result.page(), result.pageSize());
    }

    @GET
    @Path("/{seriesId}")
    public Response detail(@PathParam("seriesId") String seriesId) {
        if (!SafePath.isSafeSegment(seriesId)) {
            return badRequest("Invalid series id");
        }
        Optional<SeriesDetail> found = catalog.findSeries(seriesId);
        if (found.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("NOT_FOUND", "Unknown series"))
                    .build();
        }
        SeriesDetail d = found.get();
        SeriesDetailDto dto = new SeriesDetailDto(d.id(), d.title(), d.description(), d.status(), d.coverRelativePath());
        return Response.ok(dto).build();
    }

    @GET
    @Path("/{seriesId}/chapters")
    public Response chapters(@PathParam("seriesId") String seriesId) {
        if (!SafePath.isSafeSegment(seriesId)) {
            return badRequest("Invalid series id");
        }
        if (catalog.findSeries(seriesId).isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("NOT_FOUND", "Unknown series"))
                    .build();
        }
        List<ChapterDto> chapters =
                catalog.listChapters(seriesId).stream().map(V1SeriesResource::toChapterDto).toList();
        return Response.ok(new ChapterListResponse(seriesId, chapters)).build();
    }

    @GET
    @Path("/{seriesId}/chapters/{chapterId}/pages")
    public Response pages(@PathParam("seriesId") String seriesId, @PathParam("chapterId") String chapterId) {
        if (!SafePath.isSafeSegment(seriesId) || !SafePath.isSafeSegment(chapterId)) {
            return badRequest("Invalid series or chapter id");
        }
        if (catalog.findSeries(seriesId).isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("NOT_FOUND", "Unknown series"))
                    .build();
        }
        Optional<Integer> count = catalog.getPageCount(seriesId, chapterId);
        if (count.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("NOT_FOUND", "Unknown chapter"))
                    .build();
        }
        List<PageRefDto> refs = new ArrayList<>();
        for (int i = 0; i < count.get(); i++) {
            String url = "/v1/content/series/" + seriesId + "/chapters/" + chapterId + "/images/" + i;
            refs.add(new PageRefDto(i, url));
        }
        return Response.ok(new PageListResponse(seriesId, chapterId, refs)).build();
    }

    private static ChapterDto toChapterDto(ChapterInfo c) {
        return new ChapterDto(c.id(), c.title(), c.sortIndex());
    }

    private static SeriesSort parseSort(String raw) {
        if (raw == null) {
            return SeriesSort.POPULAR;
        }
        return switch (raw.trim().toLowerCase()) {
            case "latest" -> SeriesSort.LATEST;
            default -> SeriesSort.POPULAR;
        };
    }

    private static Response badRequest(String message) {
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse("BAD_REQUEST", message))
                .build();
    }
}
