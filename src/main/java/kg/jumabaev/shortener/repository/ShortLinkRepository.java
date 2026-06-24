package kg.jumabaev.shortener.repository;

import kg.jumabaev.shortener.entity.ShortLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ShortLinkRepository extends JpaRepository<ShortLink, UUID>, JpaSpecificationExecutor<ShortLink> {

    Optional<ShortLink> findByShortCode(String shortCode);

    @Query("SELECT shortLink.id FROM ShortLink shortLink WHERE shortLink.shortCode = :shortCode")
    Optional<UUID> findIdByShortCode(@Param("shortCode") String shortCode);

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
