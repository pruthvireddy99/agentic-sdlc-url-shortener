package com.example.agenticurl.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "click_events")
public class ClickEvent {
    @Id
    private UUID id;

    @Column(name = "short_code", nullable = false, length = 32)
    private String shortCode;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "user_agent_hash", length = 128)
    private String userAgentHash;

    @Column(name = "referrer_domain", length = 255)
    private String referrerDomain;

    protected ClickEvent() {
    }

    public ClickEvent(UUID id, String shortCode, Instant occurredAt, String userAgentHash, String referrerDomain) {
        this.id = id;
        this.shortCode = shortCode;
        this.occurredAt = occurredAt;
        this.userAgentHash = userAgentHash;
        this.referrerDomain = referrerDomain;
    }

    public UUID getId() { return id; }
    public String getShortCode() { return shortCode; }
    public Instant getOccurredAt() { return occurredAt; }
    public String getUserAgentHash() { return userAgentHash; }
    public String getReferrerDomain() { return referrerDomain; }
}
