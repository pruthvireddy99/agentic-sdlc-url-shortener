package com.example.agenticurl.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "short_urls")
public class ShortUrl {
    @Id
    private UUID id;

    @Column(name = "short_code", nullable = false, unique = true, length = 32)
    private String shortCode;

    @Column(name = "original_url", nullable = false, length = 2048)
    private String originalUrl;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "click_count", nullable = false)
    private long clickCount;

    @Column(name = "idempotency_key", unique = true, length = 128)
    private String idempotencyKey;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected ShortUrl() {
    }

    public ShortUrl(UUID id, String shortCode, String originalUrl, Instant createdAt, Instant expiresAt, String idempotencyKey) {
        this.id = id;
        this.shortCode = shortCode;
        this.originalUrl = originalUrl;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.idempotencyKey = idempotencyKey;
        this.clickCount = 0;
    }

    public UUID getId() { return id; }
    public String getShortCode() { return shortCode; }
    public String getOriginalUrl() { return originalUrl; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public long getClickCount() { return clickCount; }
    public String getIdempotencyKey() { return idempotencyKey; }

    public void incrementClickCount() { clickCount++; }
    public boolean isExpired(Instant now) { return expiresAt != null && expiresAt.isBefore(now); }
}
