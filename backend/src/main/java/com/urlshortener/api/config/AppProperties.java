package com.urlshortener.api.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String baseUrl, Cors cors, Cache cache, RateLimit rateLimit, List<String> reservedAliases) {

    public record Cors(String allowedOrigin) {}

    public record Cache(int ttlSeconds, int maxSize) {}

    public record RateLimit(int requestsPerMinute) {}
}
