package com.urlshortener.api.service;

import com.urlshortener.api.config.AppProperties;
import com.urlshortener.api.config.CacheConfig;
import com.urlshortener.api.domain.UrlEntity;
import com.urlshortener.api.dto.CreateUrlRequest;
import com.urlshortener.api.exception.AliasConflictException;
import com.urlshortener.api.exception.UrlGoneException;
import com.urlshortener.api.exception.UrlNotFoundException;
import com.urlshortener.api.repository.UrlRepository;
import com.urlshortener.api.validation.AliasValidator;
import com.urlshortener.api.validation.LongUrlValidator;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UrlService {

    private final UrlRepository urlRepository;
    private final ShortCodeEncoder encoder;
    private final LongUrlValidator longUrlValidator;
    private final AliasValidator aliasValidator;

    public UrlService(
            UrlRepository urlRepository,
            ShortCodeEncoder encoder,
            LongUrlValidator longUrlValidator,
            AliasValidator aliasValidator,
            AppProperties properties) {
        this.urlRepository = urlRepository;
        this.encoder = encoder;
        this.longUrlValidator = longUrlValidator;
        this.aliasValidator = aliasValidator;
    }

    @Transactional
    public UrlEntity createShortUrl(CreateUrlRequest request, String createdBy) {
        longUrlValidator.validate(request.longUrl());

        boolean customAlias = request.alias() != null && !request.alias().isBlank();
        if (customAlias) {
            aliasValidator.validate(request.alias());
            if (urlRepository.existsByShortCodeAndActiveTrue(request.alias())) {
                throw new AliasConflictException(request.alias());
            }
        }

        long id = urlRepository.nextId();
        String shortCode = customAlias ? request.alias() : encoder.encode(id);
        UrlEntity entity =
                UrlEntity.createNew(id, shortCode, request.longUrl(), createdBy, request.expiresAt());

        try {
            // Flushed immediately so a racing insert on the same alias surfaces as a constraint
            // violation here, inside this try block, rather than at end-of-transaction commit.
            return urlRepository.saveAndFlush(entity);
        } catch (DataIntegrityViolationException e) {
            throw new AliasConflictException(shortCode);
        }
    }

    /**
     * Looks up regardless of active status so a deactivated or expired code can be told apart
     * from one that never existed: missing row -&gt; 404, inactive/expired row -&gt; 410 (HLD §8).
     */
    @Cacheable(cacheNames = CacheConfig.SHORT_URL_CACHE, key = "#shortCode")
    @Transactional(readOnly = true)
    public UrlEntity getActiveForRedirect(String shortCode) {
        UrlEntity entity =
                urlRepository
                        .findByShortCodeIncludingInactive(shortCode)
                        .orElseThrow(() -> new UrlNotFoundException(shortCode));
        if (!entity.isActive() || entity.isExpired()) {
            throw new UrlGoneException(shortCode);
        }
        return entity;
    }

    @Transactional(readOnly = true)
    public UrlEntity getDetail(String shortCode) {
        return urlRepository
                .findByShortCodeIncludingInactive(shortCode)
                .orElseThrow(() -> new UrlNotFoundException(shortCode));
    }

    @Transactional(readOnly = true)
    public Page<UrlEntity> listActive(Pageable pageable) {
        return urlRepository.findByActiveTrueOrderByCreatedAtDesc(pageable);
    }

    @CacheEvict(cacheNames = CacheConfig.SHORT_URL_CACHE, key = "#shortCode")
    @Transactional
    public void deactivate(String shortCode) {
        UrlEntity entity =
                urlRepository
                        .findByShortCodeAndActiveTrue(shortCode)
                        .orElseThrow(() -> new UrlNotFoundException(shortCode));
        // Entity is managed within this transaction; JPA dirty-checking flushes the update
        // on commit without an explicit save() call.
        entity.deactivate();
    }
}
