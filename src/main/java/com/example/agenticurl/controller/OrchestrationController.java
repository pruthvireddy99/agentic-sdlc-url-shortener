package com.example.agenticurl.controller;

import com.example.agenticurl.domain.AuditEvent;
import com.example.agenticurl.domain.NodeStatus;
import com.example.agenticurl.domain.OrchestrationNode;
import com.example.agenticurl.domain.OrchestrationRun;
import com.example.agenticurl.domain.RunScenario;
import com.example.agenticurl.orchestration.OrchestrationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/agent-runs")
public class OrchestrationController {
    private final OrchestrationService orchestrationService;

    public OrchestrationController(OrchestrationService orchestrationService) {
        this.orchestrationService = orchestrationService;
    }

    @PostMapping
    public ResponseEntity<RunSummary> create(@Valid @RequestBody CreateRunRequest request) {
        UUID id = orchestrationService.createRun(request.scenario(), request.requirement(), request.initialClarification());
        orchestrationService.execute(id);
        return ResponseEntity.accepted().body(summary(id));
    }

    @GetMapping("/{id}")
    public RunSummary get(@PathVariable UUID id) { return summary(id); }

    @GetMapping("/{id}/audit")
    public List<AuditEvent> audit(@PathVariable UUID id) { return orchestrationService.getAudit(id); }

    @PostMapping("/{id}/approve")
    public ResponseEntity<RunSummary> approve(@PathVariable UUID id, @Valid @RequestBody ApprovalRequest request) {
        orchestrationService.approve(id, request.approvalToken(), request.comment());
        orchestrationService.execute(id);
        return ResponseEntity.accepted().body(summary(id));
    }

    @PostMapping("/{id}/clarify")
    public ResponseEntity<RunSummary> clarify(@PathVariable UUID id, @Valid @RequestBody ClarificationRequest request) {
        orchestrationService.clarify(id, request.clarification());
        orchestrationService.execute(id);
        return ResponseEntity.accepted().body(summary(id));
    }

    @PostMapping("/{id}/replan")
    public ResponseEntity<RunSummary> replan(@PathVariable UUID id, @Valid @RequestBody ReplanRequest request) {
        orchestrationService.replan(id, request.reason(), request.changedAssumption());
        orchestrationService.execute(id);
        return ResponseEntity.accepted().body(summary(id));
    }

    @PostMapping("/{id}/fallback")
    public ResponseEntity<RunSummary> fallback(@PathVariable UUID id, @Valid @RequestBody FallbackRequest request) {
        orchestrationService.fallback(id, request.nodeKey(), request.reason());
        orchestrationService.execute(id);
        return ResponseEntity.accepted().body(summary(id));
    }

    @PostMapping("/{id}/stop")
    public ResponseEntity<RunSummary> stop(@PathVariable UUID id, @RequestBody(required = false) StopRequest request) {
        orchestrationService.stop(id, request == null ? null : request.reason());
        return ResponseEntity.ok(summary(id));
    }

    @PostMapping("/{id}/rollback")
    public ResponseEntity<RunSummary> rollback(@PathVariable UUID id, @Valid @RequestBody RollbackRequest request) {
        orchestrationService.rollback(id, request.reason());
        return ResponseEntity.ok(summary(id));
    }

    private RunSummary summary(UUID id) {
        OrchestrationRun run = orchestrationService.getRun(id);
        List<OrchestrationNode> nodes = orchestrationService.getNodes(id);
        return new RunSummary(run.getId(), run.getScenario(), run.getStatus(), run.getPlanVersion(), run.getRequirement(),
                run.getContextJson(), run.getCreatedAt(), run.getUpdatedAt(),
                nodes.stream().map(NodeSummary::from).toList());
    }

    public record CreateRunRequest(@NotNull RunScenario scenario, @NotBlank String requirement, String initialClarification) {}
    public record ApprovalRequest(@NotBlank String approvalToken, String comment) {}
    public record ClarificationRequest(@NotBlank String clarification) {}
    public record ReplanRequest(@NotBlank String reason, String changedAssumption) {}
    public record FallbackRequest(@jakarta.validation.constraints.NotBlank String nodeKey, @jakarta.validation.constraints.NotBlank String reason) {}
    public record StopRequest(String reason) {}
    public record RollbackRequest(@NotBlank String reason) {}

    public record RunSummary(UUID id, RunScenario scenario, com.example.agenticurl.domain.RunStatus status, int planVersion,
                             String requirement, String contextJson, Instant createdAt, Instant updatedAt, List<NodeSummary> nodes) {}

    public record NodeSummary(UUID id, String nodeKey, com.example.agenticurl.domain.NodeType nodeType,
                              NodeStatus status, String dependencies, int attempt, int maxAttempts,
                              String outputJson, String errorMessage, Instant startedAt, Instant completedAt) {
        static NodeSummary from(OrchestrationNode n) {
            return new NodeSummary(n.getId(), n.getNodeKey(), n.getNodeType(), n.getStatus(), n.getDependenciesCsv(),
                    n.getAttempt(), n.getMaxAttempts(), n.getOutputJson(), n.getErrorMessage(), n.getStartedAt(), n.getCompletedAt());
        }
    }
}
