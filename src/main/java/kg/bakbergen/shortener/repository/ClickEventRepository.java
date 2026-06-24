package kg.bakbergen.shortener.repository;

import kg.bakbergen.shortener.entity.ClickEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ClickEventRepository extends JpaRepository<ClickEvent, UUID> {
}
