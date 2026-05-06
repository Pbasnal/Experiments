package org.example.repository;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import org.example.domain.ChapterInfo;
import org.example.domain.PageAsset;
import org.example.domain.PagedResult;
import org.example.domain.SeriesDetail;
import org.example.domain.SeriesSort;
import org.example.domain.SeriesSummary;

public interface ComicCatalogRepository {

    PagedResult<SeriesSummary> listSeries(SeriesSort sort, int page, int pageSize);

    PagedResult<SeriesSummary> searchSeries(String query, int page, int pageSize);

    Optional<SeriesDetail> findSeries(String seriesId);

    List<ChapterInfo> listChapters(String seriesId);

    Optional<Integer> getPageCount(String seriesId, String chapterId);

    Optional<PageAsset> openPage(String seriesId, String chapterId, int pageIndex);

    Optional<PageAsset> openCover(String seriesId);
}
