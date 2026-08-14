package com.example.agenticurl.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "orchestration_nodes")
public class OrchestrationNode {
    @Id
    private UUID id;

    @Column(name = "run_id", nullable = false)
    private UUID runId;

    @Column(name = "node_key", nullable = false, length = 100)
    private String nodeKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "node_type", nullable = false, length = 64)
    private NodeType nodeType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private NodeStatus status;

    @Column(name = "dependencies_csv", nullable = false, length = 2000)
    private String dependenciesCsv;

    @Column(nullable = false)
    private int attempt;

    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts;

    @Column(name = "output_json", columnDefinition = "text")
    private String outputJson;

    @Column(name = "error_message", length = 4000)
    private String errorMessage;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected OrchestrationNode() {
    }

    public OrchestrationNode(UUID id, UUID runId, String nodeKey, NodeType nodeType,
                             NodeStatus status, String dependenciesCsv, int maxAttempts) {
        this.id = id;
        this.runId = runId;
        this.nodeKey = nodeKey;
        this.nodeType = nodeType;
        this.status = status;
        this.dependenciesCsv = dependenciesCsv;
        this.maxAttempts = maxAttempts;
    }

    public UUID getId() { return id; }
    public UUID getRunId() { return runId; }
    public String getNodeKey() { return nodeKey; }
    public NodeType getNodeType() { return nodeType; }
    public NodeStatus getStatus() { return status; }
    public String getDependenciesCsv() { return dependenciesCsv; }
    public int getAttempt() { return attempt; }
    public int getMaxAttempts() { return maxAttempts; }
    public String getOutputJson() { return outputJson; }
    public String getErrorMessage() { return errorMessage; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getCompletedAt() { return completedAt; }

    public void markRunning(Instant now) { status = NodeStatus.RUNNING; attempt++; startedAt = now; errorMessage = null; }
    public void markSucceeded(String output, Instant now) { status = NodeStatus.SUCCEEDED; outputJson = output; completedAt = now; }
    public void markFailed(String error, Instant now) { status = NodeStatus.FAILED; errorMessage = error; completedAt = now; }
    public void markAwaitingApproval() { status = NodeStatus.AWAITING_APPROVAL; }
    public void markAwaitingClarification() { status = NodeStatus.AWAITING_CLARIFICATION; }
    public void markBlocked(String reason) { status = NodeStatus.BLOCKED; errorMessage = reason; completedAt = Instant.now(); }
    public void markRolledBack(String reason) { status = NodeStatus.ROLLED_BACK; errorMessage = reason; completedAt = Instant.now(); }
    public void resetToPending() { status = NodeStatus.PENDING; errorMessage = null; outputJson = null; startedAt = null; completedAt = null; }
}
