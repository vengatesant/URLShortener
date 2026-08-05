package com.urlshortener.api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.Instant;
import org.springframework.data.domain.Persistable;

/**
 * The {@code id} is pre-fetched from {@code urls_id_seq} by the service layer (see
 * {@code UrlRepository#nextId}) so the short code can be base62-encoded and stored in the same
 * INSERT. That manual id assignment means Spring Data can't infer "is this new" from an id being
 * null, so this implements {@link Persistable} to say so explicitly and avoid an extra
 * merge/SELECT on every create.
 */
@Entity
@Table(name = "urls")
public class UrlEntity implements Persistable<Long> {

    @Id
    private Long id;

    @Column(name = "short_code", nullable = false, length = 16)
    private String shortCode;

    @Column(name = "long_url", nullable = false, columnDefinition = "text")
    private String longUrl;

    @Column(name = "created_by", length = 64)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Transient
    private boolean isNew = false;

    protected UrlEntity() {
        // JPA
    }

    public static UrlEntity createNew(
            Long id, String shortCode, String longUrl, String createdBy, Instant expiresAt) {
        UrlEntity entity = new UrlEntity();
        entity.id = id;
        entity.shortCode = shortCode;
        entity.longUrl = longUrl;
        entity.createdBy = createdBy;
        entity.createdAt = Instant.now();
        entity.expiresAt = expiresAt;
        entity.active = true;
        entity.isNew = true;
        return entity;
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostPersist
    @PostLoad
    void markNotNew() {
        this.isNew = false;
    }

    public String getShortCode() {
        return shortCode;
    }

    public String getLongUrl() {
        return longUrl;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(Instant.now());
    }

    public void deactivate() {
        this.active = false;
    }
}
