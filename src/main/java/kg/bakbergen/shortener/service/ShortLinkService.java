package kg.bakbergen.shortener.service;

import kg.bakbergen.shortener.cache.ShortLinkCacheService;
import kg.bakbergen.shortener.config.AppProperties;
import kg.bakbergen.shortener.dto.CreateShortLinkRequest;
import kg.bakbergen.shortener.dto.DailyClickResponse;
import kg.bakbergen.shortener.dto.PageResponse;
import kg.bakbergen.shortener.dto.ShortLinkResponse;
import kg.bakbergen.shortener.dto.ShortLinkStatsResponse;
import kg.bakbergen.shortener.dto.UpdateShortLinkRequest;
import kg.bakbergen.shortener.entity.ShortLink;
import kg.bakbergen.shortener.exception.BadRequestException;
import kg.bakbergen.shortener.exception.ConflictException;
import kg.bakbergen.shortener.exception.NotFoundException;
import kg.bakbergen.shortener.exception.ShortCodeGenerationException;
import kg.bakbergen.shortener.mapper.ShortLinkMapper;
import kg.bakbergen.shortener.repository.ClickEventRepository;
import kg.bakbergen.shortener.repository.ShortLinkRepository;
import kg.bakbergen.shortener.util.CodeGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ShortLinkService {

    private static final int MAX_GENERATION_ATTEMPTS = 5;
    private static final Set<String> ALLOWED_SORT_PROPERTIES = Set.of(
            "id", "originalUrl", "shortCode", "title", "active", "clickCount",
            "createdAt", "updatedAt", "expiresAt", "lastAccessedAt"
    );

    private final ShortLinkRepository shortLinkRepository;
    private final ClickEventRepository clickEventRepository;
    private final ShortLinkMapper shortLinkMapper;
    private final CodeGenerator codeGenerator;
    private final AppProperties appProperties;
    private final ShortLinkCacheService cacheService;

    @Transactional
    public ShortLinkResponse create(CreateShortLinkRequest request) {
        boolean customAliasRequested = request.customAlias() != null;
        String shortCode = customAliasRequested
                ? validateCustomAliasAvailability(request.customAlias())
                : generateUniqueShortCode();

        ShortLink shortLink = ShortLink.builder()
                .originalUrl(request.originalUrl())
                .shortCode(shortCode)
                .title(request.title())
                .expiresAt(request.expiresAt())
                .build();

        try {
            ShortLink savedLink = shortLinkRepository.saveAndFlush(shortLink);
            return shortLinkMapper.toResponse(savedLink, buildShortUrl(savedLink.getShortCode()));
        } catch (DataIntegrityViolationException exception) {
            if (customAliasRequested) {
                throw new ConflictException("Custom alias '%s' already exists".formatted(shortCode));
            }
            throw new ShortCodeGenerationException("Could not persist a unique short code", exception);
        }
    }

    @Transactional(readOnly = true)
    public ShortLinkResponse get(String shortCode) {
        ShortLink shortLink = findByShortCode(shortCode);
        return toResponse(shortLink);
    }

    @Transactional(readOnly = true)
    public PageResponse<ShortLinkResponse> list(String search, Pageable pageable) {
        validateSort(pageable);
        Specification<ShortLink> specification = buildSearchSpecification(search);
        Page<ShortLinkResponse> result = shortLinkRepository.findAll(specification, pageable)
                .map(this::toResponse);
        return PageResponse.from(result);
    }

    @Transactional(readOnly = true)
    public ShortLinkStatsResponse getStats(String shortCode) {
        ShortLink shortLink = findByShortCode(shortCode);
        List<DailyClickResponse> dailyClicks = clickEventRepository.findDailyClicks(shortLink.getId()).stream()
                .map(row -> new DailyClickResponse(row.getDate(), row.getClicks()))
                .toList();

        return new ShortLinkStatsResponse(
                shortLink.getShortCode(),
                shortLink.getOriginalUrl(),
                shortLink.getClickCount(),
                shortLink.getCreatedAt(),
                shortLink.getLastAccessedAt(),
                dailyClicks
        );
    }

    @Transactional
    public ShortLinkResponse update(String shortCode, UpdateShortLinkRequest request) {
        ShortLink shortLink = findByShortCode(shortCode);

        if (request.isOriginalUrlPresent()) {
            shortLink.setOriginalUrl(request.getOriginalUrl());
        }
        if (request.isTitlePresent()) {
            shortLink.setTitle(request.getTitle());
        }
        if (request.isActivePresent()) {
            shortLink.setActive(request.getActive());
        }
        if (request.isExpiresAtPresent()) {
            shortLink.setExpiresAt(request.getExpiresAt());
        }

        ShortLink savedLink = shortLinkRepository.saveAndFlush(shortLink);
        cacheService.evict(shortCode);
        return toResponse(savedLink);
    }

    @Transactional
    public void delete(String shortCode) {
        ShortLink shortLink = findByShortCode(shortCode);
        shortLink.setActive(false);
        shortLinkRepository.saveAndFlush(shortLink);
        cacheService.evict(shortCode);
    }

    private String validateCustomAliasAvailability(String customAlias) {
        if (shortLinkRepository.existsByShortCode(customAlias)) {
            throw new ConflictException("Custom alias '%s' already exists".formatted(customAlias));
        }
        return customAlias;
    }

    private String generateUniqueShortCode() {
        for (int attempt = 0; attempt < MAX_GENERATION_ATTEMPTS; attempt++) {
            String candidate = codeGenerator.generate();
            if (!shortLinkRepository.existsByShortCode(candidate)) {
                return candidate;
            }
        }
        throw new ShortCodeGenerationException(
                "Could not generate a unique short code after %d attempts".formatted(MAX_GENERATION_ATTEMPTS)
        );
    }

    private ShortLink findByShortCode(String shortCode) {
        return shortLinkRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new NotFoundException("Short link '%s' was not found".formatted(shortCode)));
    }

    private void validateSort(Pageable pageable) {
        pageable.getSort().forEach(order -> {
            if (!ALLOWED_SORT_PROPERTIES.contains(order.getProperty())) {
                throw new BadRequestException("Unsupported sort property: " + order.getProperty());
            }
        });
    }

    private Specification<ShortLink> buildSearchSpecification(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        String pattern = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
        return (root, query, criteriaBuilder) -> criteriaBuilder.or(
                criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), pattern),
                criteriaBuilder.like(criteriaBuilder.lower(root.get("originalUrl")), pattern),
                criteriaBuilder.like(criteriaBuilder.lower(root.get("shortCode")), pattern)
        );
    }

    private ShortLinkResponse toResponse(ShortLink shortLink) {
        return shortLinkMapper.toResponse(shortLink, buildShortUrl(shortLink.getShortCode()));
    }

    private String buildShortUrl(String shortCode) {
        return appProperties.baseUrl() + "/" + shortCode;
    }
}
