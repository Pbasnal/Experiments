package org.example.api;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;
import java.util.Optional;
import org.example.api.dto.ErrorResponse;
import org.example.domain.PageAsset;
import org.example.repository.ComicCatalogRepository;
import org.example.repository.LocalCatalog;
import org.example.util.SafePath;

@Path("/v1/content/series/{seriesId}")
public class V1SeriesContentResource {

    private final ComicCatalogRepository catalog;

    @Inject
    public V1SeriesContentResource(@LocalCatalog ComicCatalogRepository catalog) {
        this.catalog = catalog;
    }

    @GET
    @Path("/cover")
    public Response cover(@PathParam("seriesId") String seriesId) {
        if (!SafePath.isSafeSegment(seriesId)) {
            return notFound();
        }
        Optional<PageAsset> asset = catalog.openCover(seriesId);
        if (asset.isEmpty()) {
            return notFound();
        }
        PageAsset a = asset.get();
        return Response.ok(a.stream(), a.mediaType()).build();
    }

    @GET
    @Path("/chapters/{chapterId}/images/{pageIndex}")
    public Response page(
            @PathParam("seriesId") String seriesId,
            @PathParam("chapterId") String chapterId,
            @PathParam("pageIndex") int pageIndex) {
        if (!SafePath.isSafeSegment(seriesId) || !SafePath.isSafeSegment(chapterId)) {
            return notFound();
        }
        Optional<PageAsset> asset = catalog.openPage(seriesId, chapterId, pageIndex);
        if (asset.isEmpty()) {
            return notFound();
        }
        PageAsset a = asset.get();
        return Response.ok(a.stream(), a.mediaType()).build();
    }

    private static Response notFound() {
        return Response.status(Response.Status.NOT_FOUND)
                .entity(new ErrorResponse("NOT_FOUND", "Resource not found"))
                .type("application/json")
                .build();
    }
}
