package com.urlshortener.api.controller;

import com.urlshortener.api.config.AppProperties;
import com.urlshortener.api.domain.UrlEntity;
import com.urlshortener.api.dto.CreateUrlRequest;
import com.urlshortener.api.dto.PageResponse;
import com.urlshortener.api.dto.UrlResponse;
import com.urlshortener.api.dto.UrlStatsResponse;
import com.urlshortener.api.service.AnalyticsService;
import com.urlshortener.api.service.UrlService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/urls")
public class UrlController {

    private final UrlService urlService;
    private final AnalyticsService analyticsService;
    private final AppProperties properties;

    public UrlController(UrlService urlService, AnalyticsService analyticsService, AppProperties properties) {
        this.urlService = urlService;
        this.analyticsService = analyticsService;
        this.properties = properties;
    }

    @PostMapping
    public ResponseEntity<UrlResponse> create(@Valid @RequestBody CreateUrlRequest request) {
        UrlEntity entity = urlService.createShortUrl(request, null);
        return ResponseEntity.status(HttpStatus.CREATED).body(UrlResponse.from(entity, properties.baseUrl()));
    }

    @GetMapping
    public PageResponse<UrlResponse> list(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        Page<UrlEntity> result = urlService.listActive(PageRequest.of(page, Math.min(size, 100)));
        return PageResponse.of(result.map(entity -> UrlResponse.from(entity, properties.baseUrl())));
    }

    @GetMapping("/{shortCode}")
    public UrlResponse detail(@PathVariable String shortCode) {
        return UrlResponse.from(urlService.getDetail(shortCode), properties.baseUrl());
    }

    @GetMapping("/{shortCode}/stats")
    public UrlStatsResponse stats(@PathVariable String shortCode) {
        return analyticsService.getStats(shortCode);
    }

    @DeleteMapping("/{shortCode}")
    public ResponseEntity<Void> deactivate(@PathVariable String shortCode) {
        urlService.deactivate(shortCode);
        return ResponseEntity.noContent().build();
    }
}
