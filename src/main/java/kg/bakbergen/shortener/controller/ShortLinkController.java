package kg.bakbergen.shortener.controller;

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
public class ShortLinkController {

    private final ShortLinkService shortLinkService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ShortLinkResponse create(@Valid @RequestBody CreateShortLinkRequest request) {
        return shortLinkService.create(request);
    }

    @GetMapping("/{shortCode}")
    public ShortLinkResponse get(@PathVariable String shortCode) {
        return shortLinkService.get(shortCode);
    }

    @GetMapping
    public PageResponse<ShortLinkResponse> list(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return shortLinkService.list(search, pageable);
    }

    @GetMapping("/{shortCode}/stats")
    public ShortLinkStatsResponse getStats(@PathVariable String shortCode) {
        return shortLinkService.getStats(shortCode);
    }

    @PatchMapping("/{shortCode}")
    public ShortLinkResponse update(
            @PathVariable String shortCode,
            @Valid @RequestBody UpdateShortLinkRequest request
    ) {
        return shortLinkService.update(shortCode, request);
    }

    @DeleteMapping("/{shortCode}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String shortCode) {
        shortLinkService.delete(shortCode);
    }
}
