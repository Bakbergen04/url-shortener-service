package kg.bakbergen.shortener.controller;

import jakarta.validation.Valid;
import kg.bakbergen.shortener.dto.CreateShortLinkRequest;
import kg.bakbergen.shortener.dto.ShortLinkResponse;
import kg.bakbergen.shortener.service.ShortLinkService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
}
