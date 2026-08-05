package com.urlshortener.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.urlshortener.api.config.AppProperties;
import com.urlshortener.api.domain.UrlEntity;
import com.urlshortener.api.dto.CreateUrlRequest;
import com.urlshortener.api.exception.AliasConflictException;
import com.urlshortener.api.exception.UrlGoneException;
import com.urlshortener.api.exception.UrlNotFoundException;
import com.urlshortener.api.repository.UrlRepository;
import com.urlshortener.api.validation.AliasValidator;
import com.urlshortener.api.validation.LongUrlValidator;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class UrlServiceTest {

    @Mock private UrlRepository urlRepository;

    private UrlService urlService;

    @BeforeEach
    void setUp() {
        AppProperties properties =
                new AppProperties(
                        "http://localhost:8080",
                        new AppProperties.Cors("http://localhost:5173"),
                        new AppProperties.Cache(300, 10_000),
                        new AppProperties.RateLimit(20),
                        List.of("api", "r", "admin"));
        urlService =
                new UrlService(
                        urlRepository, new ShortCodeEncoder(), new LongUrlValidator(), new AliasValidator(properties), properties);
    }

    @Test
    void generatesBase62CodeWhenNoAliasGiven() {
        when(urlRepository.nextId()).thenReturn(62L);
        when(urlRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        UrlEntity result = urlService.createShortUrl(new CreateUrlRequest("https://example.com", null, null), null);

        assertThat(result.getShortCode()).isEqualTo("10");
        assertThat(result.getLongUrl()).isEqualTo("https://example.com");
    }

    @Test
    void usesCustomAliasWhenAvailable() {
        when(urlRepository.existsByShortCodeAndActiveTrue("my-link")).thenReturn(false);
        when(urlRepository.nextId()).thenReturn(1L);
        when(urlRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        UrlEntity result =
                urlService.createShortUrl(new CreateUrlRequest("https://example.com", "my-link", null), null);

        assertThat(result.getShortCode()).isEqualTo("my-link");
    }

    @Test
    void rejectsAliasThatIsAlreadyActive() {
        when(urlRepository.existsByShortCodeAndActiveTrue("taken")).thenReturn(true);

        assertThatThrownBy(
                        () -> urlService.createShortUrl(new CreateUrlRequest("https://example.com", "taken", null), null))
                .isInstanceOf(AliasConflictException.class);
    }

    @Test
    void mapsConcurrentAliasRaceToAliasConflict() {
        when(urlRepository.existsByShortCodeAndActiveTrue("race")).thenReturn(false);
        when(urlRepository.nextId()).thenReturn(1L);
        when(urlRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("dup"));

        assertThatThrownBy(
                        () -> urlService.createShortUrl(new CreateUrlRequest("https://example.com", "race", null), null))
                .isInstanceOf(AliasConflictException.class);
    }

    @Test
    void rejectsReservedAlias() {
        assertThatThrownBy(
                        () -> urlService.createShortUrl(new CreateUrlRequest("https://example.com", "api", null), null))
                .hasMessageContaining("reserved");
        verify(urlRepository, org.mockito.Mockito.never()).saveAndFlush(any());
    }

    @Test
    void redirectLookupThrowsNotFoundWhenMissing() {
        when(urlRepository.findByShortCodeIncludingInactive("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> urlService.getActiveForRedirect("missing"))
                .isInstanceOf(UrlNotFoundException.class);
    }

    @Test
    void redirectLookupThrowsGoneWhenExpired() {
        UrlEntity expired =
                UrlEntity.createNew(1L, "abc123", "https://example.com", null, Instant.now().minus(1, ChronoUnit.DAYS));
        when(urlRepository.findByShortCodeIncludingInactive("abc123")).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> urlService.getActiveForRedirect("abc123")).isInstanceOf(UrlGoneException.class);
    }

    @Test
    void redirectLookupThrowsGoneWhenDeactivated() {
        UrlEntity entity = UrlEntity.createNew(1L, "abc123", "https://example.com", null, null);
        entity.deactivate();
        when(urlRepository.findByShortCodeIncludingInactive("abc123")).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> urlService.getActiveForRedirect("abc123")).isInstanceOf(UrlGoneException.class);
    }
}
