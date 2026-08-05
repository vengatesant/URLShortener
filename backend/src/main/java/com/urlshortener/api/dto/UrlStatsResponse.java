package com.urlshortener.api.dto;

import java.time.LocalDate;
import java.util.List;

public record UrlStatsResponse(
        String shortCode,
        long totalClicks,
        List<DailyCount> byDay,
        List<ReferrerCount> byReferrer) {

    public record DailyCount(LocalDate day, long clicks) {}

    public record ReferrerCount(String referrer, long clicks) {}
}
