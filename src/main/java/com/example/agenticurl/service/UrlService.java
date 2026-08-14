package com.example.agenticurl.service;

import com.example.agenticurl.config.ShortUrlProperties;
import com.example.agenticurl.domain.ClickEvent;
import com.example.agenticurl.domain.ShortUrl;
import com.example.agenticurl.exception.NotFoundException;
import com.example.agenticurl.repository.ClickEventRepository;
import com.example.agenticurl.repository.ShortUrlRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class UrlService {
    private static final char[] BASE62 = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();
    private final ShortUrlRepository shortUrlRepository;
    private final ClickEventRepository clickEventRepository;
    private final ShortUrlProperties properties;
    private final MeterRegistry meterRegistry;

    public UrlService(ShortUrlRepository shortUrlRepository, ClickEventRepository clickEventRepository,
                      ShortUrlProperties properties, MeterRegistry meterRegistry) {
        this.shortUrlRepository = shortUrlRepository;
        this.clickEventRepository = clickEventRepository;
        this.properties = properties;
        this.meterRegistry = meterRegistry;
    }

    @Transactional
    public ShortenResult shorten(String originalUrl, String idempotencyKey, Integer ttlHours) {
        validateUrl(originalUrl);
        if (originalUrl.length() > properties.maxOriginalUrlLength()) {
            throw new IllegalArgumentException("originalUrl exceeds the configured maximum length");
        }
        if (idempotencyKey != null) {
            var existing = shortUrlRepository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) return toResult(existing.get());
        }

        Instant now = Instant.now();
        Instant expiresAt = ttlHours == null ? null : now.plus(ttlHours, ChronoUnit.HOURS);
        for (int i = 0; i < 8; i++) {
            String code = randomCode(properties.codeLength());
            try {
                var saved = shortUrlRepository.saveAndFlush(
                        new ShortUrl(UUID.randomUUID(), code, originalUrl, now, expiresAt, idempotencyKey));
                meterRegistry.counter("urlshortener.urls.created").increment();
                return toResult(saved);
            } catch (DataIntegrityViolationException ex) {
                if (idempotencyKey != null) {
                    var existing = shortUrlRepository.findByIdempotencyKey(idempotencyKey);
                    if (existing.isPresent()) return toResult(existing.get());
                }
            }
        }
        throw new IllegalStateException("Unable to allocate a unique short code after bounded retries");
    }

    @Transactional
    public RedirectResult resolve(String code, String userAgent, String referer) {
        var url = shortUrlRepository.findByShortCode(code)
                .orElseThrow(() -> new NotFoundException("Short URL does not exist"));
        if (url.isExpired(Instant.now())) throw new NotFoundException("Short URL has expired");

        String uaHash = sha256(userAgent == null ? "" : userAgent);
        String domain = extractReferrerDomain(referer);
        clickEventRepository.save(new ClickEvent(UUID.randomUUID(), code, Instant.now(), uaHash, domain));
        shortUrlRepository.incrementClickCount(code);
        meterRegistry.counter("urlshortener.redirects").increment();
        return new RedirectResult(URI.create(url.getOriginalUrl()), url.getOriginalUrl());
    }

    @Transactional(readOnly = true)
    public AnalyticsResult analytics(String code) {
        var url = shortUrlRepository.findByShortCode(code)
                .orElseThrow(() -> new NotFoundException("Short URL does not exist"));
        Instant now = Instant.now();
        long last24h = clickEventRepository.countSince(code, now.minus(24, ChronoUnit.HOURS));
        long last7d = clickEventRepository.countSince(code, now.minus(7, ChronoUnit.DAYS));
        return new AnalyticsResult(code, url.getOriginalUrl(), url.getCreatedAt(), url.getExpiresAt(),
                url.getClickCount(), last24h, last7d);
    }

    private ShortenResult toResult(ShortUrl url) {
        return new ShortenResult(url.getShortCode(), properties.publicBaseUrl() + "/" + url.getShortCode(),
                url.getOriginalUrl(), url.getCreatedAt(), url.getExpiresAt(), url.getIdempotencyKey());
    }

    private static void validateUrl(String value) {
        Objects.requireNonNull(value, "originalUrl");
        URI uri = URI.create(value);
        if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalArgumentException("Only http and https URLs are supported");
        }
        if (uri.getHost() == null) throw new IllegalArgumentException("URL must contain a host");
    }

    private static String randomCode(int length) {
        var random = ThreadLocalRandom.current();
        var builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) builder.append(BASE62[random.nextInt(BASE62.length)]);
        return builder.toString();
    }

    private static String sha256(String value) {
        try {
            var md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String extractReferrerDomain(String referer) {
        if (referer == null || referer.isBlank()) return null;
        try { return URI.create(referer).getHost(); } catch (IllegalArgumentException ex) { return null; }
    }

    public record ShortenResult(String code, String shortUrl, String originalUrl, Instant createdAt,
                                Instant expiresAt, String idempotencyKey) {}
    public record RedirectResult(URI location, String originalUrl) {}
    public record AnalyticsResult(String code, String originalUrl, Instant createdAt, Instant expiresAt,
                                  long totalClicks, long clicksLast24Hours, long clicksLast7Days) {}
}
