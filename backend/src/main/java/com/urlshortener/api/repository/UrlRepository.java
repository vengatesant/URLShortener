package com.urlshortener.api.repository;

import com.urlshortener.api.domain.UrlEntity;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UrlRepository extends JpaRepository<UrlEntity, Long> {

    Optional<UrlEntity> findByShortCodeAndActiveTrue(String shortCode);

    boolean existsByShortCodeAndActiveTrue(String shortCode);

    Page<UrlEntity> findByActiveTrueOrderByCreatedAtDesc(Pageable pageable);

    /**
     * Pre-fetches the next value from {@code urls_id_seq} so the service layer can base62-encode
     * it and persist the row with its short code in a single INSERT (see {@link UrlEntity}).
     */
    @Query(value = "SELECT nextval('urls_id_seq')", nativeQuery = true)
    long nextId();

    @Query("select u from UrlEntity u where u.shortCode = :shortCode")
    Optional<UrlEntity> findByShortCodeIncludingInactive(@Param("shortCode") String shortCode);
}
