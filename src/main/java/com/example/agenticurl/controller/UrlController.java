package com.example.agenticurl.controller;

import com.example.agenticurl.service.UrlService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/urls")
public class UrlController {
    private final UrlService urlService;

    public UrlController(UrlService urlService) {
        this.urlService = urlService;
    }

    @PostMapping
    public ResponseEntity<UrlService.ShortenResult> shorten(
            @Valid @RequestBody ShortenRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        var result = urlService.shorten(request.originalUrl(), idempotencyKey, request.ttlHours());
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath().path("/{code}").buildAndExpand(result.code()).toUri();
        return ResponseEntity.created(location).body(result);
    }

    @GetMapping("/{code}/analytics")
    public UrlService.AnalyticsResult analytics(@PathVariable String code) {
        return urlService.analytics(code);
    }

    public record ShortenRequest(
            @NotBlank
            @Pattern(regexp = "https?://.+", message = "originalUrl must start with http:// or https://")
            String originalUrl,
            @Min(1) @Max(8760) Integer ttlHours) {
    }
}
