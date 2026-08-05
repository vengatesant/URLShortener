package com.urlshortener.api.repository;

import com.urlshortener.api.domain.ClickEvent;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClickEventRepository extends JpaRepository<ClickEvent, Long> {

    long countByUrlId(Long urlId);

    @Query(
            value =
                    "select date_trunc('day', clicked_at)::date as day, count(*) as clicks "
                            + "from click_events where url_id = :urlId "
                            + "group by day order by day",
            nativeQuery = true)
    List<DailyCount> dailyCounts(@Param("urlId") Long urlId);

    @Query(
            value =
                    "select coalesce(nullif(referrer, ''), 'direct') as referrer, count(*) as clicks "
                            + "from click_events where url_id = :urlId "
                            + "group by referrer order by clicks desc limit 10",
            nativeQuery = true)
    List<ReferrerCount> topReferrers(@Param("urlId") Long urlId);

    interface DailyCount {
        LocalDate getDay();

        long getClicks();
    }

    interface ReferrerCount {
        String getReferrer();

        long getClicks();
    }
}
