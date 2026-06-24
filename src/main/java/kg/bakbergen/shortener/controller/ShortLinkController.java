package kg.bakbergen.shortener.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import jakarta.validation.Valid;
import kg.bakbergen.shortener.dto.CreateShortLinkRequest;
import kg.bakbergen.shortener.dto.PageResponse;
import kg.bakbergen.shortener.dto.ShortLinkResponse;
import kg.bakbergen.shortener.dto.ShortLinkStatsResponse;
import kg.bakbergen.shortener.dto.UpdateShortLinkRequest;
import kg.bakbergen.shortener.service.ShortLinkService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/links")
@RequiredArgsConstructor
@Tag(name = "Short links", description = "Create, inspect, update, delete, and analyze short links")
public class ShortLinkController {

    private final ShortLinkService shortLinkService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a short link")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Short link created"),
            @ApiResponse(responseCode = "400", description = "Request validation failed"),
            @ApiResponse(responseCode = "409", description = "Custom alias already exists"),
            @ApiResponse(responseCode = "500", description = "Short code generation failed")
    })
    public ShortLinkResponse create(@Valid @RequestBody CreateShortLinkRequest request) {
        return shortLinkService.create(request);
    }

    @GetMapping("/{shortCode}")
    @Operation(summary = "Get short link information")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Short link returned"),
            @ApiResponse(responseCode = "404", description = "Short link not found")
    })
    public ShortLinkResponse get(@PathVariable String shortCode) {
        return shortLinkService.get(shortCode);
    }

    @GetMapping
    @Operation(summary = "List and search short links")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page of short links returned"),
            @ApiResponse(responseCode = "400", description = "Pagination or sorting parameters are invalid")
    })
    public PageResponse<ShortLinkResponse> list(
            @RequestParam(required = false) String search,
            @ParameterObject
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return shortLinkService.list(search, pageable);
    }

    @GetMapping("/{shortCode}/stats")
    @Operation(summary = "Get click statistics for a short link")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Statistics returned"),
            @ApiResponse(responseCode = "404", description = "Short link not found")
    })
    public ShortLinkStatsResponse getStats(@PathVariable String shortCode) {
        return shortLinkService.getStats(shortCode);
    }

    @PatchMapping("/{shortCode}")
    @Operation(summary = "Partially update a short link")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Short link updated"),
            @ApiResponse(responseCode = "400", description = "Request validation failed"),
            @ApiResponse(responseCode = "404", description = "Short link not found")
    })
    public ShortLinkResponse update(
            @PathVariable String shortCode,
            @Valid @RequestBody UpdateShortLinkRequest request
    ) {
        return shortLinkService.update(shortCode, request);
    }

    @DeleteMapping("/{shortCode}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Deactivate a short link")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Short link deactivated"),
            @ApiResponse(responseCode = "404", description = "Short link not found")
    })
    public void delete(@PathVariable String shortCode) {
        shortLinkService.delete(shortCode);
    }
}
