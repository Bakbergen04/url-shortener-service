package kg.jumabaev.shortener.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import kg.jumabaev.shortener.dto.DailyClickResponse;
import kg.jumabaev.shortener.dto.PageResponse;
import kg.jumabaev.shortener.dto.ShortLinkStatsResponse;
import kg.jumabaev.shortener.exception.GlobalExceptionHandler;
import kg.jumabaev.shortener.service.ShortLinkService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ShortLinkControllerTest {

    private ShortLinkService shortLinkService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        shortLinkService = mock(ShortLinkService.class);
        Clock clock = Clock.fixed(Instant.parse("2026-06-25T00:00:00Z"), ZoneOffset.UTC);
        ObjectMapper objectMapper = JsonMapper.builder()
                .findAndAddModules()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();

        mockMvc = MockMvcBuilders.standaloneSetup(new ShortLinkController(shortLinkService))
                .setControllerAdvice(new GlobalExceptionHandler(clock))
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setMessageConverters(new org.springframework.http.converter.json.MappingJackson2HttpMessageConverter(
                        objectMapper
                ))
                .build();
    }

    @Test
    void createRejectsInvalidUrl() throws Exception {
        mockMvc.perform(post("/api/v1/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "originalUrl": "javascript:alert(1)",
                                  "customAlias": "valid-alias"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.path").value("/api/v1/links"));

        verifyNoInteractions(shortLinkService);
    }

    @Test
    void returnsStatistics() throws Exception {
        ShortLinkStatsResponse response = new ShortLinkStatsResponse(
                "my-link",
                "https://example.com",
                3,
                Instant.parse("2026-06-20T00:00:00Z"),
                Instant.parse("2026-06-21T00:00:00Z"),
                List.of(new DailyClickResponse(LocalDate.parse("2026-06-21"), 3))
        );
        when(shortLinkService.getStats("my-link")).thenReturn(response);

        mockMvc.perform(get("/api/v1/links/my-link/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shortCode").value("my-link"))
                .andExpect(jsonPath("$.clickCount").value(3))
                .andExpect(jsonPath("$.dailyClicks[0].date").value("2026-06-21"))
                .andExpect(jsonPath("$.dailyClicks[0].clicks").value(3));
    }

    @Test
    void passesPaginationSearchAndSortToService() throws Exception {
        PageResponse<kg.jumabaev.shortener.dto.ShortLinkResponse> response = new PageResponse<>(
                List.of(), 2, 5, 0, 0, false, true
        );
        when(shortLinkService.list(eq("example"), org.mockito.ArgumentMatchers.any(Pageable.class)))
                .thenReturn(response);

        mockMvc.perform(get("/api/v1/links")
                        .param("search", "example")
                        .param("page", "2")
                        .param("size", "5")
                        .param("sort", "clickCount,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.content").isArray());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(shortLinkService).list(eq("example"), pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageNumber()).isEqualTo(2);
        assertThat(pageable.getPageSize()).isEqualTo(5);
        assertThat(pageable.getSort().getOrderFor("clickCount")).isNotNull();
        assertThat(pageable.getSort().getOrderFor("clickCount").isDescending()).isTrue();
    }
}
