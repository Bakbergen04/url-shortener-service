package kg.bakbergen.shortener.repository;

import kg.bakbergen.shortener.entity.ShortLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ShortLinkRepository extends JpaRepository<ShortLink, UUID> {

    Optional<ShortLink> findByShortCode(String shortCode);

    boolean existsByShortCode(String shortCode);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE short_links
            SET click_count = click_count + 1,
                last_accessed_at = NOW()
            WHERE id = :id
            """, nativeQuery = true)
    int incrementClickCount(@Param("id") UUID id);
}
