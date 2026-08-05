package com.urlshortener.api.controller;

import com.urlshortener.api.domain.UrlEntity;
import com.urlshortener.api.service.ClickTrackingService;
import com.urlshortener.api.service.UrlService;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** The redirect hot path (HLD §1, §6) — never routed through the SPA. */
@RestController
public class RedirectController {

    private final UrlService urlService;
    private final ClickTrackingService clickTrackingService;

    public RedirectController(UrlService urlService, ClickTrackingService clickTrackingService) {
        this.urlService = urlService;
        this.clickTrackingService = clickTrackingService;
    }

    @GetMapping("/r/{shortCode}")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode, HttpServletRequest request) {
        UrlEntity entity = urlService.getActiveForRedirect(shortCode);

        clickTrackingService.record(
                entity.getId(), request.getHeader(HttpHeaders.REFERER), request.getHeader(HttpHeaders.USER_AGENT), null);

        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(entity.getLongUrl())).build();
    }
}
