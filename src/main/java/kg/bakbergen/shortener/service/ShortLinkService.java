package kg.bakbergen.shortener.service;

import kg.bakbergen.shortener.config.AppProperties;
import kg.bakbergen.shortener.dto.CreateShortLinkRequest;
import kg.bakbergen.shortener.dto.ShortLinkResponse;
import kg.bakbergen.shortener.entity.ShortLink;
import kg.bakbergen.shortener.exception.ConflictException;
import kg.bakbergen.shortener.exception.ShortCodeGenerationException;
import kg.bakbergen.shortener.mapper.ShortLinkMapper;
import kg.bakbergen.shortener.repository.ShortLinkRepository;
import kg.bakbergen.shortener.util.CodeGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ShortLinkService {

    private static final int MAX_GENERATION_ATTEMPTS = 5;

    private final ShortLinkRepository shortLinkRepository;
    private final ShortLinkMapper shortLinkMapper;
    private final CodeGenerator codeGenerator;
    private final AppProperties appProperties;

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

    private String buildShortUrl(String shortCode) {
        return appProperties.baseUrl() + "/" + shortCode;
    }
}
