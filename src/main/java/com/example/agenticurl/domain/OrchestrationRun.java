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
@Table(name = "orchestration_runs")
public class OrchestrationRun {
    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private RunScenario scenario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private RunStatus status;

    @Column(name = "plan_version", nullable = false)
    private int planVersion;

    @Column(nullable = false, columnDefinition = "text")
    private String requirement;

    @Column(name = "context_json", nullable = false, columnDefinition = "text")
    private String contextJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "stopped_at")
    private Instant stoppedAt;

    @Column(name = "stop_reason", length = 1000)
    private String stopReason;

    protected OrchestrationRun() {
    }

    public OrchestrationRun(UUID id, RunScenario scenario, RunStatus status, int planVersion,
                            String requirement, String contextJson, Instant now) {
        this.id = id;
        this.scenario = scenario;
        this.status = status;
        this.planVersion = planVersion;
        this.requirement = requirement;
        this.contextJson = contextJson;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public UUID getId() { return id; }
    public RunScenario getScenario() { return scenario; }
    public RunStatus getStatus() { return status; }
    public int getPlanVersion() { return planVersion; }
    public String getRequirement() { return requirement; }
    public String getContextJson() { return contextJson; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getStoppedAt() { return stoppedAt; }
    public String getStopReason() { return stopReason; }

    public void setStatus(RunStatus status) { this.status = status; touch(); }
    public void setPlanVersion(int planVersion) { this.planVersion = planVersion; touch(); }
    public void setContextJson(String contextJson) { this.contextJson = contextJson; touch(); }
    public void stop(String reason, Instant now) { this.status = RunStatus.STOPPED; this.stopReason = reason; this.stoppedAt = now; this.updatedAt = now; }
    public void touch() { this.updatedAt = Instant.now(); }
}
