package kg.jumabaev.shortener.service;

import kg.jumabaev.shortener.cache.ShortLinkCacheService;
import kg.jumabaev.shortener.config.AppProperties;
import kg.jumabaev.shortener.dto.CreateShortLinkRequest;
import kg.jumabaev.shortener.dto.ShortLinkResponse;
import kg.jumabaev.shortener.dto.UpdateShortLinkRequest;
import kg.jumabaev.shortener.entity.ShortLink;
import kg.jumabaev.shortener.exception.ConflictException;
import kg.jumabaev.shortener.mapper.ShortLinkMapper;
import kg.jumabaev.shortener.repository.ClickEventRepository;
import kg.jumabaev.shortener.repository.ShortLinkRepository;
import kg.jumabaev.shortener.util.CodeGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShortLinkServiceTest {

    @Mock
    private ShortLinkRepository shortLinkRepository;

    @Mock
    private ClickEventRepository clickEventRepository;

    @Mock
    private ShortLinkMapper shortLinkMapper;

    @Mock
    private CodeGenerator codeGenerator;

    @Mock
    private AppProperties appProperties;

    @Mock
    private ShortLinkCacheService cacheService;

    @InjectMocks
    private ShortLinkService service;

    @BeforeEach
    void setUp() {
        lenient().when(appProperties.baseUrl()).thenReturn("http://localhost:8080");
    }

    @Test
    void createsLinkWithCustomAlias() {
        CreateShortLinkRequest request = new CreateShortLinkRequest(
                "https://example.com/path", "my-link", "Example", null
        );
        ShortLinkResponse expected = response("my-link");
        when(shortLinkRepository.existsByShortCode("my-link")).thenReturn(false);
        when(shortLinkRepository.saveAndFlush(any(ShortLink.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(shortLinkMapper.toResponse(any(ShortLink.class), eq("http://localhost:8080/my-link")))
                .thenReturn(expected);

        ShortLinkResponse actual = service.create(request);

        assertThat(actual).isSameAs(expected);
        verify(shortLinkRepository).saveAndFlush(any(ShortLink.class));
        verify(codeGenerator, never()).generate();
    }

    @Test
    void retriesGeneratedCodeWhenCandidateAlreadyExists() {
        CreateShortLinkRequest request = new CreateShortLinkRequest("https://example.com", null, null, null);
        ShortLinkResponse expected = response("free222");
        when(codeGenerator.generate()).thenReturn("used111", "free222");
        when(shortLinkRepository.existsByShortCode("used111")).thenReturn(true);
        when(shortLinkRepository.existsByShortCode("free222")).thenReturn(false);
        when(shortLinkRepository.saveAndFlush(any(ShortLink.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(shortLinkMapper.toResponse(any(ShortLink.class), eq("http://localhost:8080/free222")))
                .thenReturn(expected);

        ShortLinkResponse actual = service.create(request);

        assertThat(actual.shortCode()).isEqualTo("free222");
        verify(codeGenerator, org.mockito.Mockito.times(2)).generate();
    }

    @Test
    void rejectsExistingCustomAlias() {
        CreateShortLinkRequest request = new CreateShortLinkRequest(
                "https://example.com", "my-link", null, null
        );
        when(shortLinkRepository.existsByShortCode("my-link")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("my-link");

        verify(shortLinkRepository, never()).saveAndFlush(any());
    }

    @Test
    void updateEvictsCachedValue() {
        ShortLink shortLink = ShortLink.builder()
                .id(UUID.randomUUID())
                .shortCode("my-link")
                .originalUrl("https://example.com")
                .active(true)
                .build();
        UpdateShortLinkRequest request = new UpdateShortLinkRequest();
        request.setTitle("Updated title");
        when(shortLinkRepository.findByShortCode("my-link")).thenReturn(Optional.of(shortLink));
        when(shortLinkRepository.saveAndFlush(shortLink)).thenReturn(shortLink);
        when(shortLinkMapper.toResponse(eq(shortLink), eq("http://localhost:8080/my-link")))
                .thenReturn(response("my-link"));

        service.update("my-link", request);

        assertThat(shortLink.getTitle()).isEqualTo("Updated title");
        verify(cacheService).evict("my-link");
    }

    private ShortLinkResponse response(String shortCode) {
        return new ShortLinkResponse(
                UUID.randomUUID(),
                "https://example.com",
                shortCode,
                "http://localhost:8080/" + shortCode,
                null,
                true,
                0,
                Instant.parse("2026-01-01T00:00:00Z"),
                null
        );
    }
}
