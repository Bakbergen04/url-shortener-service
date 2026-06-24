package kg.bakbergen.shortener.repository;

import kg.bakbergen.shortener.entity.ClickEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ClickEventRepository extends JpaRepository<ClickEvent, UUID> {

    @Query(value = """
            SELECT CAST(clicked_at AT TIME ZONE 'UTC' AS DATE) AS date,
                   COUNT(*) AS clicks
            FROM click_events
            WHERE short_link_id = :shortLinkId
            GROUP BY CAST(clicked_at AT TIME ZONE 'UTC' AS DATE)
            ORDER BY date
            """, nativeQuery = true)
    List<DailyClickProjection> findDailyClicks(@Param("shortLinkId") UUID shortLinkId);

    interface DailyClickProjection {

        LocalDate getDate();

        long getClicks();
    }
}
