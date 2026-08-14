package com.example.agenticurl.orchestration;

import com.example.agenticurl.domain.NodeType;
import com.example.agenticurl.domain.OrchestrationNode;
import com.example.agenticurl.domain.OrchestrationRun;
import com.example.agenticurl.repository.OrchestrationNodeRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class NodeExecutionService {
    private final OrchestrationNodeRepository nodeRepository;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    public NodeExecutionService(OrchestrationNodeRepository nodeRepository, ObjectMapper objectMapper,
                                MeterRegistry meterRegistry) {
        this.nodeRepository = nodeRepository;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
    }

    @Transactional(noRollbackFor = {ClarificationRequiredException.class, IllegalStateException.class})
    public String execute(UUID runId, String nodeKey, Map<String, Object> context) {
        OrchestrationNode node = nodeRepository.findByRunIdOrderById(runId).stream()
                .filter(n -> n.getNodeKey().equals(nodeKey))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Node not found: " + nodeKey));
        if (node.getAttempt() >= node.getMaxAttempts()) {
            throw new IllegalStateException("Retry budget exhausted for node " + nodeKey);
        }

        Instant start = Instant.now();
        node.markRunning(start);
        nodeRepository.save(node);
        try {
            Map<String, Object> output = executeAgent(node.getNodeType(), node.getNodeKey(), context);
            String json = objectMapper.writeValueAsString(output);
            node.markSucceeded(json, Instant.now());
            nodeRepository.save(node);
            Counter.builder("agentic.node.success")
                    .tag("node_type", node.getNodeType().name())
                    .register(meterRegistry).increment();
            Timer.builder("agentic.node.latency")
                    .tag("node_type", node.getNodeType().name())
                    .register(meterRegistry).record(java.time.Duration.between(start, Instant.now()));
            return json;
        } catch (ClarificationRequiredException ex) {
            node.markAwaitingClarification();
            nodeRepository.save(node);
            throw ex;
        } catch (Exception ex) {
            node.markFailed(ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage(), Instant.now());
            nodeRepository.save(node);
            Counter.builder("agentic.node.failure")
                    .tag("node_type", node.getNodeType().name())
                    .register(meterRegistry).increment();
            throw new IllegalStateException("Node " + nodeKey + " failed: " + ex.getMessage(), ex);
        }
    }

    private Map<String, Object> executeAgent(NodeType type, String nodeKey, Map<String, Object> context) {
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("agent", agentName(type));
        output.put("node", nodeKey);
        output.put("timestamp", Instant.now().toString());
        output.put("decisionLineage", context.getOrDefault("decisionLineage", java.util.List.of()));

        switch (type) {
            case REQUIREMENTS -> {
                output.put("normalizedProblem", "Build a governed URL shortener with creation, redirect, analytics and reliability capabilities.");
                output.put("acceptanceCriteria", java.util.List.of(
                        "HTTP/HTTPS URLs are shortened with collision-safe codes",
                        "Repeated create requests with the same idempotency key are safe",
                        "Redirects record analytics without leaking raw user-agent values",
                        "Operational health and metrics are exposed",
                        "Agentic workflow has approval, stop, retry, rollback and audit controls"
                ));
                output.put("ambiguities", java.util.List.of(
                        "Retention period for analytics",
                        "Production identity provider and deployment target",
                        "Exact external LLM provider contract"
                ));
            }
            case CODEBASE_REASONING -> {
                output.put("impactedAreas", java.util.List.of("URL API", "Persistence layer", "Analytics event flow", "Orchestration control plane", "Observability"));
                output.put("dataFlow", "HTTP -> controller -> service -> JPA repository -> PostgreSQL; click event persisted alongside counter update");
            }
            case IMPACT_ANALYSIS -> {
                output.put("changeSurface", java.util.List.of("API contract", "short_urls schema", "click_events schema", "orchestration nodes", "tests"));
                output.put("regressionFocus", java.util.List.of("idempotency", "redirect correctness", "expiration", "parallel workflow synchronization"));
            }
            case AMBIGUITY_REVIEW -> {
                if (context.get("clarification") == null) {
                    throw new ClarificationRequiredException("Human clarification is required before architecture can proceed");
                }
                output.put("resolvedAmbiguity", context.get("clarification"));
                output.put("decision", "Proceed with defaults explicitly recorded in decision lineage");
            }
            case ARCHITECTURE -> {
                output.put("components", java.util.List.of("Spring Boot REST API", "PostgreSQL", "Flyway", "Micrometer/Actuator", "Governed Agent Orchestrator"));
                output.put("patterns", java.util.List.of("Layered architecture", "Idempotency key", "bounded retry", "human-in-the-loop", "audit trail", "dependency graph", "safe stop"));
                output.put("keyDecision", "Use a stateful graph executor rather than a linear prompt chain so parallel work and synchronization are explicit");
            }
            case IMPLEMENTATION -> {
                output.put("artifacts", java.util.List.of("REST controllers", "domain services", "JPA entities/repositories", "orchestration engine", "governance endpoints"));
                output.put("qualityGates", java.util.List.of("validation", "transaction boundaries", "bounded loops", "policy checks", "metrics"));
            }
            case TESTING -> {
                output.put("tests", java.util.List.of("URL validation", "idempotency", "redirect analytics", "expiration", "workflow dependency gating", "approval gating", "safe stop", "replan"));
                output.put("validation", "Unit tests plus optional PostgreSQL Testcontainers integration coverage");
            }
            case DOCUMENTATION -> {
                output.put("documents", java.util.List.of("README", "architecture overview", "scenario walkthroughs", "API contract", "risk register", "runbook"));
            }
            case RELEASE_READINESS -> {
                output.put("checks", java.util.List.of("tests green", "approval required", "no unresolved policy violation", "rollback path defined", "observability available"));
                output.put("risk", "Production LLM credentials and deployment credentials are intentionally outside autonomous scope");
            }
            case RELEASE -> {
                output.put("action", "Release approved engineering outcome");
                output.put("safeScope", "Prototype artifacts only; no infrastructure mutation is performed autonomously");
            }
            case HUMAN_APPROVAL -> {
                output.put("action", "approval checkpoint");
            }
            case REPLAN -> {
                output.put("action", "rebuild downstream plan after upstream output change");
            }
            default -> output.put("action", "no-op");
        }
        return output;
    }

    private String agentName(NodeType type) {
        return switch (type) {
            case REQUIREMENTS, AMBIGUITY_REVIEW -> "requirements-agent";
            case CODEBASE_REASONING, IMPACT_ANALYSIS -> "codebase-analysis-agent";
            case ARCHITECTURE -> "architecture-agent";
            case IMPLEMENTATION -> "implementation-agent";
            case TESTING -> "validation-agent";
            case DOCUMENTATION -> "documentation-agent";
            case RELEASE_READINESS -> "release-readiness-agent";
            case HUMAN_APPROVAL -> "governance-controller";
            case RELEASE -> "release-agent";
            case REPLAN -> "planner-agent";
        };
    }

    public static class ClarificationRequiredException extends RuntimeException {
        public ClarificationRequiredException(String message) { super(message); }
    }
}
