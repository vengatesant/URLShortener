package com.urlshortener.api.dto;

import com.urlshortener.api.domain.UrlEntity;
import java.time.Instant;

public record UrlResponse(
        String shortCode,
        String shortUrl,
        String longUrl,
        Instant createdAt,
        Instant expiresAt,
        boolean active) {

    public static UrlResponse from(UrlEntity entity, String baseUrl) {
        return new UrlResponse(
                entity.getShortCode(),
                baseUrl + "/r/" + entity.getShortCode(),
                entity.getLongUrl(),
                entity.getCreatedAt(),
                entity.getExpiresAt(),
                entity.isActive());
    }
}
