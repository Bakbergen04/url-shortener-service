package kg.jumabaev.shortener.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kg.jumabaev.shortener.entity.ShortLink;
import kg.jumabaev.shortener.repository.ClickEventRepository;
import kg.jumabaev.shortener.repository.ShortLinkRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class ShortLinkIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:16-alpine")
    );

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>(
            DockerImageName.parse("redis:7.4-alpine")
    ).withExposedPorts(6379);

    @DynamicPropertySource
    static void configureInfrastructure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add("app.base-url", () -> "http://localhost:8080");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoSpyBean
    private ShortLinkRepository shortLinkRepository;

    @Autowired
    private ClickEventRepository clickEventRepository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @BeforeEach
    void cleanState() {
        clickEventRepository.deleteAll();
        shortLinkRepository.deleteAll();
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb();
        clearInvocations(shortLinkRepository);
    }

    @Test
    void createPersistsShortLinkInPostgres() throws Exception {
        String code = createLink("persist1", "https://example.com/long-path");

        ShortLink saved = shortLinkRepository.findByShortCode(code).orElseThrow();
        assertThat(saved.getOriginalUrl()).isEqualTo("https://example.com/long-path");
        assertThat(saved.isActive()).isTrue();
        assertThat(saved.getClickCount()).isZero();
    }

    @Test
    void redirectReturns302AndRecordsClick() throws Exception {
        String code = createLink("redirect1", "https://example.com/target");

        mockMvc.perform(get("/{shortCode}", code)
                        .header("User-Agent", "integration-test")
                        .header("Referer", "https://referrer.example"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://example.com/target"));

        ShortLink updated = shortLinkRepository.findByShortCode(code).orElseThrow();
        assertThat(updated.getClickCount()).isEqualTo(1);
        assertThat(updated.getLastAccessedAt()).isNotNull();
        assertThat(clickEventRepository.count()).isEqualTo(1);
    }

    @Test
    void expiredLinkReturns410() throws Exception {
        shortLinkRepository.saveAndFlush(ShortLink.builder()
                .shortCode("expired1")
                .originalUrl("https://example.com")
                .expiresAt(Instant.now().minusSeconds(60))
                .build());

        mockMvc.perform(get("/expired1"))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.status").value(410));
    }

    @Test
    void duplicateCustomAliasReturns409() throws Exception {
        createLink("duplicate", "https://example.com/one");

        mockMvc.perform(post("/api/v1/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "originalUrl": "https://example.com/two",
                                  "customAlias": "duplicate"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void secondRedirectUsesRedisCache() throws Exception {
        String code = createLink("cached01", "https://example.com/cached");
        clearInvocations(shortLinkRepository);

        mockMvc.perform(get("/{shortCode}", code)).andExpect(status().isFound());
        assertThat(redisTemplate.hasKey("short-link:" + code)).isTrue();
        mockMvc.perform(get("/{shortCode}", code)).andExpect(status().isFound());

        verify(shortLinkRepository, times(1)).findByShortCode(code);
        assertThat(shortLinkRepository.findByShortCode(code).orElseThrow().getClickCount()).isEqualTo(2);
    }

    @Test
    void updateAndDeleteEvictRedisCache() throws Exception {
        String code = createLink("evict001", "https://example.com/cache");
        mockMvc.perform(get("/{shortCode}", code)).andExpect(status().isFound());
        assertThat(redisTemplate.hasKey("short-link:" + code)).isTrue();

        mockMvc.perform(patch("/api/v1/links/{shortCode}", code)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Updated\"}"))
                .andExpect(status().isOk());
        assertThat(redisTemplate.hasKey("short-link:" + code)).isFalse();

        mockMvc.perform(get("/{shortCode}", code)).andExpect(status().isFound());
        assertThat(redisTemplate.hasKey("short-link:" + code)).isTrue();

        mockMvc.perform(delete("/api/v1/links/{shortCode}", code))
                .andExpect(status().isNoContent());
        assertThat(redisTemplate.hasKey("short-link:" + code)).isFalse();
        mockMvc.perform(get("/{shortCode}", code)).andExpect(status().isGone());
    }

    private String createLink(String alias, String originalUrl) throws Exception {
        String response = mockMvc.perform(post("/api/v1/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "originalUrl": "%s",
                                  "customAlias": "%s"
                                }
                                """.formatted(originalUrl, alias)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode json = objectMapper.readTree(response);
        return json.get("shortCode").asText();
    }
}
