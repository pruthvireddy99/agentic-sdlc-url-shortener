package com.example.agenticurl.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_events")
public class AuditEvent {
    @Id
    private UUID id;
    @Column(name = "run_id", nullable = false)
    private UUID runId;
    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;
    @Column(nullable = false, length = 128)
    private String actor;
    @Column(name = "node_key", length = 100)
    private String nodeKey;
    @Column(name = "event_at", nullable = false)
    private Instant eventAt;
    @Column(name = "payload_json", columnDefinition = "text")
    private String payloadJson;

    protected AuditEvent() {
    }

    public AuditEvent(UUID id, UUID runId, String eventType, String actor, String nodeKey, Instant eventAt, String payloadJson) {
        this.id = id;
        this.runId = runId;
        this.eventType = eventType;
        this.actor = actor;
        this.nodeKey = nodeKey;
        this.eventAt = eventAt;
        this.payloadJson = payloadJson;
    }

    public UUID getId() { return id; }
    public UUID getRunId() { return runId; }
    public String getEventType() { return eventType; }
    public String getActor() { return actor; }
    public String getNodeKey() { return nodeKey; }
    public Instant getEventAt() { return eventAt; }
    public String getPayloadJson() { return payloadJson; }
}
