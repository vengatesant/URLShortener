package com.urlshortener.api.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.TimeUnit;
import org.springframework.boot.autoconfigure.cache.CacheManagerCustomizer;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Cache-aside store for {@code shortCode -> longUrl} on the redirect hot path (see HLD §8). */
@Configuration
public class CacheConfig {

    public static final String SHORT_URL_CACHE = "shortUrlCache";

    @Bean
    public CacheManagerCustomizer<CaffeineCacheManager> caffeineCacheManagerCustomizer(
            AppProperties properties) {
        return cacheManager -> {
            cacheManager.setCacheNames(java.util.List.of(SHORT_URL_CACHE));
            cacheManager.setCaffeine(
                    Caffeine.newBuilder()
                            .maximumSize(properties.cache().maxSize())
                            .expireAfterWrite(properties.cache().ttlSeconds(), TimeUnit.SECONDS));
        };
    }
}
