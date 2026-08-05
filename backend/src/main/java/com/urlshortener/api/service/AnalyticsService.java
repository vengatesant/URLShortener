package com.urlshortener.api.service;

import com.urlshortener.api.domain.UrlEntity;
import com.urlshortener.api.dto.UrlStatsResponse;
import com.urlshortener.api.repository.ClickEventRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalyticsService {

    private final UrlService urlService;
    private final ClickEventRepository clickEventRepository;

    public AnalyticsService(UrlService urlService, ClickEventRepository clickEventRepository) {
        this.urlService = urlService;
        this.clickEventRepository = clickEventRepository;
    }

    @Transactional(readOnly = true)
    public UrlStatsResponse getStats(String shortCode) {
        UrlEntity entity = urlService.getDetail(shortCode);
        long total = clickEventRepository.countByUrlId(entity.getId());

        List<UrlStatsResponse.DailyCount> byDay =
                clickEventRepository.dailyCounts(entity.getId()).stream()
                        .map(row -> new UrlStatsResponse.DailyCount(row.getDay(), row.getClicks()))
                        .toList();

        List<UrlStatsResponse.ReferrerCount> byReferrer =
                clickEventRepository.topReferrers(entity.getId()).stream()
                        .map(row -> new UrlStatsResponse.ReferrerCount(row.getReferrer(), row.getClicks()))
                        .toList();

        return new UrlStatsResponse(shortCode, total, byDay, byReferrer);
    }
}
