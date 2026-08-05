package com.urlshortener.api.service;

import com.urlshortener.api.config.AsyncConfig;
import com.urlshortener.api.domain.ClickEvent;
import com.urlshortener.api.repository.ClickEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/** Fire-and-forget click recording so it never adds latency to the redirect response (HLD §6, Fig. 4). */
@Service
public class ClickTrackingService {

    private static final Logger log = LoggerFactory.getLogger(ClickTrackingService.class);

    private final ClickEventRepository clickEventRepository;

    public ClickTrackingService(ClickEventRepository clickEventRepository) {
        this.clickEventRepository = clickEventRepository;
    }

    @Async(AsyncConfig.CLICK_EVENT_EXECUTOR)
    public void record(Long urlId, String referrer, String userAgent, String country) {
        try {
            clickEventRepository.save(new ClickEvent(urlId, referrer, userAgent, country));
        } catch (Exception e) {
            // A dropped click must never surface as a failed redirect; the browser already has
            // its 302 by the time this runs.
            log.warn("Failed to record click for urlId={}", urlId, e);
        }
    }
}
