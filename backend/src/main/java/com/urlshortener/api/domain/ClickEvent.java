package com.urlshortener.api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Deliberately not a JPA relationship to {@link UrlEntity} — the write path (see
 * ClickTrackingService) only ever needs the foreign key, and avoiding the association keeps
 * the async insert from ever touching the parent row.
 */
@Entity
@Table(name = "click_events")
public class ClickEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "url_id", nullable = false)
    private Long urlId;

    @Column(name = "clicked_at", nullable = false)
    private Instant clickedAt;

    @Column(name = "referrer")
    private String referrer;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "country", length = 2)
    private String country;

    protected ClickEvent() {
        // JPA
    }

    public ClickEvent(Long urlId, String referrer, String userAgent, String country) {
        this.urlId = urlId;
        this.clickedAt = Instant.now();
        this.referrer = referrer;
        this.userAgent = userAgent;
        this.country = country;
    }

    public Long getId() {
        return id;
    }

    public Long getUrlId() {
        return urlId;
    }

    public Instant getClickedAt() {
        return clickedAt;
    }

    public String getReferrer() {
        return referrer;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public String getCountry() {
        return country;
    }
}
