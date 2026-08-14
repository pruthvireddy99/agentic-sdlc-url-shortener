package com.example.agenticurl.orchestration;

import com.example.agenticurl.config.GovernanceProperties;
import com.example.agenticurl.domain.AuditEvent;
import com.example.agenticurl.domain.NodeStatus;
import com.example.agenticurl.domain.NodeType;
import com.example.agenticurl.domain.OrchestrationNode;
import com.example.agenticurl.domain.OrchestrationRun;
import com.example.agenticurl.domain.RunScenario;
import com.example.agenticurl.domain.RunStatus;
import com.example.agenticurl.exception.InvalidStateException;
import com.example.agenticurl.exception.NotFoundException;
import com.example.agenticurl.exception.PolicyViolationException;
import com.example.agenticurl.repository.AuditEventRepository;
import com.example.agenticurl.repository.OrchestrationNodeRepository;
import com.example.agenticurl.repository.OrchestrationRunRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class OrchestrationService {
    private final OrchestrationRunRepository runRepository;
    private final OrchestrationNodeRepository nodeRepository;
    private final AuditEventRepository auditRepository;
    private final NodeExecutionService nodeExecutionService;
    private final GovernanceProperties governance;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;
    private final Executor executor = Executors.newFixedThreadPool(Math.max(4, Runtime.getRuntime().availableProcessors()));
    private final ConcurrentMap<UUID, Instant> runStartTimes = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Instant> nodeFailureTimes = new ConcurrentHashMap<>();

    public OrchestrationService(OrchestrationRunRepository runRepository,
                                OrchestrationNodeRepository nodeRepository,
                                AuditEventRepository auditRepository,
                                NodeExecutionService nodeExecutionService,
                                GovernanceProperties governance,
                                ObjectMapper objectMapper,
                                MeterRegistry meterRegistry) {
        this.runRepository = runRepository;
        this.nodeRepository = nodeRepository;
        this.auditRepository = auditRepository;
        this.nodeExecutionService = nodeExecutionService;
        this.governance = governance;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
    }

    @Transactional
    public UUID createRun(RunScenario scenario, String requirement, String initialClarification) {
        if (requirement == null || requirement.isBlank()) throw new IllegalArgumentException("requirement is required");
        UUID runId = UUID.randomUUID();
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("requirement", requirement);
        context.put("decisionLineage", new ArrayList<>());
        context.put("artifacts", new ArrayList<>());
        if (initialClarification != null && !initialClarification.isBlank()) context.put("clarification", initialClarification);

        Instant now = Instant.now();
        OrchestrationRun run = new OrchestrationRun(runId, scenario, RunStatus.CREATED, 1, requirement, toJson(context), now);
        runRepository.save(run);
        for (WorkflowNodeDefinition def : WorkflowTemplates.forScenario(scenario)) {
            nodeRepository.save(new OrchestrationNode(UUID.randomUUID(), runId, def.key(), def.type(), NodeStatus.PENDING,
                    String.join(",", def.dependencies()), def.humanGate() ? 1 : governance.maxNodeRetries() + 1));
        }
        audit(runId, "RUN_CREATED", "system", null, Map.of("scenario", scenario.name(), "planVersion", 1));
        return runId;
    }

    public void execute(UUID runId) {
        Instant started = Instant.now();
        runStartTimes.putIfAbsent(runId, started);
        Counter.builder("agentic.runs.started").register(meterRegistry).increment();
        OrchestrationRun run = getRun(runId);
        setRunStatus(runId, RunStatus.RUNNING);

        try {
            while (Duration.between(started, Instant.now()).toSeconds() < governance.maxRunDurationSeconds()) {
                OrchestrationRun current = getRun(runId);
                if (current.getStatus() == RunStatus.STOPPED || current.getStatus() == RunStatus.ROLLED_BACK) return;

                List<OrchestrationNode> nodes = nodeRepository.findByRunIdOrderById(runId);
                if (allSucceeded(nodes)) {
                    if (nodes.stream().anyMatch(n -> n.getNodeType() == NodeType.RELEASE && n.getStatus() == NodeStatus.SUCCEEDED)) {
                        audit(runId, "RELEASE_COMPLETED", "release-agent", "release", Map.of("planVersion", current.getPlanVersion()));
                    }
                    setRunStatus(runId, RunStatus.COMPLETED);
                    audit(runId, "RUN_COMPLETED", "orchestrator", null, Map.of("planVersion", current.getPlanVersion()));
                    Timer.builder("agentic.run.latency").register(meterRegistry)
                            .record(Duration.between(started, Instant.now()));
                    Counter.builder("agentic.runs.completed").register(meterRegistry).increment();
                    runStartTimes.remove(runId);
                    return;
                }
                if (nodes.stream().anyMatch(n -> n.getStatus() == NodeStatus.FAILED && n.getAttempt() >= n.getMaxAttempts())) {
                    setRunStatus(runId, RunStatus.FAILED);
                    audit(runId, "RUN_FAILED", "orchestrator", null, Map.of("reason", "retry budget exhausted"));
                    Counter.builder("agentic.runs.failed").register(meterRegistry).increment();
                    runStartTimes.remove(runId);
                    return;
                }

                Map<String, Object> context = fromJson(current.getContextJson());
                List<OrchestrationNode> ready = readyNodes(nodes);
                if (ready.isEmpty()) {
                    handleGates(runId, nodes, context);
                    OrchestrationRun refreshed = getRun(runId);
                    if (refreshed.getStatus() == RunStatus.AWAITING_APPROVAL || refreshed.getStatus() == RunStatus.AWAITING_CLARIFICATION) return;
                    Thread.sleep(50L);
                    continue;
                }

                List<CompletableFuture<NodeResult>> futures = new ArrayList<>();
                for (OrchestrationNode node : ready) {
                    futures.add(CompletableFuture.supplyAsync(() -> executeOne(runId, node, context), executor));
                }
                CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
                for (CompletableFuture<NodeResult> future : futures) {
                    NodeResult result = future.join();
                    if (result.success()) {
                        mergeNodeOutput(context, result.nodeKey(), result.outputJson());
                        audit(runId, "NODE_SUCCEEDED", "orchestrator", result.nodeKey(), Map.of("attempt", result.attempt()));
                        Instant failureAt = nodeFailureTimes.remove(runId + "|" + result.nodeKey());
                        if (failureAt != null) {
                            Timer.builder("agentic.node.recovery.latency")
                                    .tag("node", result.nodeKey())
                                    .register(meterRegistry)
                                    .record(Duration.between(failureAt, Instant.now()));
                        }
                    } else if (result.waitingForClarification()) {
                        audit(runId, "HUMAN_CLARIFICATION_REQUIRED", "governance-controller", result.nodeKey(), Map.of("message", result.error()));
                    } else {
                        audit(runId, "NODE_RETRY_SCHEDULED", "orchestrator", result.nodeKey(), Map.of("attempt", result.attempt(), "error", result.error()));
                        nodeFailureTimes.put(runId + "|" + result.nodeKey(), Instant.now());
                        Counter.builder("agentic.node.retries").tag("node", result.nodeKey()).register(meterRegistry).increment();
                        nodeRepository.findByRunIdOrderById(runId).stream()
                                .filter(n -> n.getNodeKey().equals(result.nodeKey()) && n.getStatus() == NodeStatus.FAILED)
                                .findFirst()
                                .ifPresent(n -> {
                                    if (n.getAttempt() < n.getMaxAttempts()) {
                                        n.resetToPending();
                                        nodeRepository.save(n);
                                    }
                                });
                    }
                }
                saveContext(runId, context);
            }
            stop(runId, "Maximum run duration exceeded");
        } catch (Exception ex) {
            setRunStatus(runId, RunStatus.FAILED);
            audit(runId, "RUN_FAILED", "orchestrator", null, Map.of("error", ex.getMessage() == null ? "unknown" : ex.getMessage()));
            Counter.builder("agentic.runs.failed").register(meterRegistry).increment();
            runStartTimes.remove(runId);
            throw new InvalidStateException("Orchestration failed: " + ex.getMessage());
        }
    }

    private NodeResult executeOne(UUID runId, OrchestrationNode node, Map<String, Object> context) {
        try {
            String output = nodeExecutionService.execute(runId, node.getNodeKey(), context);
            return new NodeResult(node.getNodeKey(), true, output, null, node.getAttempt() + 1, false);
        } catch (NodeExecutionService.ClarificationRequiredException ex) {
            return new NodeResult(node.getNodeKey(), false, null, ex.getMessage(), node.getAttempt(), true);
        } catch (Exception ex) {
            OrchestrationNode refreshed = nodeRepository.findById(node.getId()).orElse(node);
            return new NodeResult(node.getNodeKey(), false, null, ex.getMessage(), refreshed.getAttempt(), false);
        }
    }

    private void handleGates(UUID runId, List<OrchestrationNode> nodes, Map<String, Object> context) {
        OrchestrationRun run = getRun(runId);
        OrchestrationNode clarification = nodes.stream().filter(n -> n.getStatus() == NodeStatus.AWAITING_CLARIFICATION).findFirst().orElse(null);
        if (clarification != null) {
            setRunStatus(runId, RunStatus.AWAITING_CLARIFICATION);
            audit(runId, "HUMAN_CLARIFICATION_REQUIRED", "governance-controller", clarification.getNodeKey(),
                    Map.of("message", "Provide clarification before architecture can proceed"));
            return;
        }
        OrchestrationNode approval = nodes.stream().filter(n -> n.getNodeType() == NodeType.HUMAN_APPROVAL &&
                (n.getStatus() == NodeStatus.PENDING || n.getStatus() == NodeStatus.AWAITING_APPROVAL)).findFirst().orElse(null);
        if (approval != null && dependenciesSucceeded(approval, nodes)) {
            if (approval.getStatus() != NodeStatus.AWAITING_APPROVAL) {
                approval.markAwaitingApproval();
                nodeRepository.save(approval);
                audit(runId, "APPROVAL_REQUESTED", "governance-controller", approval.getNodeKey(),
                        Map.of("highImpact", true, "requiresHuman", true));
            }
            setRunStatus(runId, RunStatus.AWAITING_APPROVAL);
        }
    }

    @Transactional
    public void approve(UUID runId, String token, String comment) {
        requireToken(token);
        OrchestrationRun run = getRun(runId);
        if (run.getStatus() != RunStatus.AWAITING_APPROVAL) throw new InvalidStateException("Run is not awaiting approval");
        OrchestrationNode gate = nodeRepository.findByRunIdAndStatus(runId, NodeStatus.AWAITING_APPROVAL).stream().findFirst()
                .orElseThrow(() -> new InvalidStateException("No pending approval gate found"));
        gate.markSucceeded(toJson(Map.of("approved", true, "comment", comment == null ? "" : comment, "approvedAt", Instant.now().toString())), Instant.now());
        nodeRepository.save(gate);
        run.setStatus(RunStatus.RUNNING);
        runRepository.save(run);
        audit(runId, "APPROVAL_GRANTED", "human", gate.getNodeKey(), Map.of("comment", comment == null ? "" : comment));
        Counter.builder("agentic.approvals.granted").register(meterRegistry).increment();
    }

    @Transactional
    public void clarify(UUID runId, String clarification) {
        if (clarification == null || clarification.isBlank()) throw new IllegalArgumentException("clarification is required");
        OrchestrationRun run = getRun(runId);
        if (run.getStatus() != RunStatus.AWAITING_CLARIFICATION) throw new InvalidStateException("Run is not awaiting clarification");
        Map<String, Object> context = fromJson(run.getContextJson());
        context.put("clarification", clarification);
        appendDecision(context, "Human clarification supplied: " + clarification);
        run.setContextJson(toJson(context));
        run.setStatus(RunStatus.REPLANNING);
        run.setPlanVersion(run.getPlanVersion() + 1);
        runRepository.save(run);
        for (OrchestrationNode node : nodeRepository.findByRunIdOrderById(runId)) {
            if (node.getStatus() == NodeStatus.AWAITING_CLARIFICATION) node.resetToPending();
            nodeRepository.save(node);
        }
        audit(runId, "CLARIFICATION_RECEIVED", "human", "ambiguity-review", Map.of("planVersion", run.getPlanVersion()));
        audit(runId, "PLAN_REPLANNED", "planner-agent", null, Map.of("trigger", "upstream clarification changed decision context"));
        run.setStatus(RunStatus.RUNNING);
        runRepository.save(run);
    }

    @Transactional
    public void replan(UUID runId, String reason, String changedAssumption) {
        requireNonBlank(reason, "reason");
        OrchestrationRun run = getRun(runId);
        if (run.getStatus() == RunStatus.STOPPED || run.getStatus() == RunStatus.ROLLED_BACK || run.getStatus() == RunStatus.COMPLETED) {
            throw new InvalidStateException("Cannot replan a terminal run");
        }
        Map<String, Object> context = fromJson(run.getContextJson());
        appendDecision(context, "Replan: " + reason + (changedAssumption == null ? "" : " | changed assumption: " + changedAssumption));
        context.put("lastReplanReason", reason);
        if (changedAssumption != null) context.put("changedAssumption", changedAssumption);
        run.setContextJson(toJson(context));
        run.setPlanVersion(run.getPlanVersion() + 1);
        run.setStatus(RunStatus.REPLANNING);
        runRepository.save(run);

        Set<String> downstream = downstreamOf(run.getScenario(), "requirements");
        for (OrchestrationNode node : nodeRepository.findByRunIdOrderById(runId)) {
            if (downstream.contains(node.getNodeKey()) && node.getStatus() != NodeStatus.PENDING && node.getStatus() != NodeStatus.AWAITING_APPROVAL) {
                node.resetToPending();
                nodeRepository.save(node);
            }
        }
        audit(runId, "PLAN_REPLANNED", "planner-agent", null, Map.of("reason", reason, "planVersion", run.getPlanVersion()));
        Counter.builder("agentic.replans.total").tag("reason", "manual").register(meterRegistry).increment();
        run.setStatus(RunStatus.RUNNING);
        runRepository.save(run);
    }


    @Transactional
    public void fallback(UUID runId, String nodeKey, String reason) {
        requireNonBlank(nodeKey, "nodeKey");
        requireNonBlank(reason, "reason");
        OrchestrationRun run = getRun(runId);
        OrchestrationNode node = nodeRepository.findByRunIdOrderById(runId).stream()
                .filter(n -> n.getNodeKey().equals(nodeKey))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Node does not exist"));
        if (node.getNodeType() != NodeType.DOCUMENTATION) {
            throw new PolicyViolationException("Fallback is permitted only for low-impact documentation work in this prototype");
        }
        if (node.getStatus() != NodeStatus.FAILED) {
            throw new InvalidStateException("Fallback requires a failed node");
        }
        node.markSucceeded(toJson(Map.of("fallback", true, "reason", reason, "action", "retained existing documentation and recorded the gap")), Instant.now());
        nodeRepository.save(node);
        run.setStatus(RunStatus.RUNNING);
        runRepository.save(run);
        audit(runId, "FALLBACK_APPLIED", "governance-controller", nodeKey, Map.of("reason", reason));
        Counter.builder("agentic.run.fallback").register(meterRegistry).increment();
        Counter.builder("agentic.fallbacks.total").register(meterRegistry).increment();
    }

    @Transactional
    public void stop(UUID runId, String reason) {
        OrchestrationRun run = getRun(runId);
        if (run.getStatus() == RunStatus.COMPLETED || run.getStatus() == RunStatus.ROLLED_BACK) throw new InvalidStateException("Run already terminal");
        run.stop(reason == null || reason.isBlank() ? "Manual safe-stop" : reason, Instant.now());
        runRepository.save(run);
        for (OrchestrationNode node : nodeRepository.findByRunIdOrderById(runId)) {
            if (node.getStatus() == NodeStatus.PENDING || node.getStatus() == NodeStatus.RUNNING ||
                    node.getStatus() == NodeStatus.AWAITING_APPROVAL || node.getStatus() == NodeStatus.AWAITING_CLARIFICATION) {
                node.markBlocked("Safe-stop: " + run.getStopReason());
                nodeRepository.save(node);
            }
        }
        audit(runId, "SAFE_STOP", "human", null, Map.of("reason", run.getStopReason()));
        Counter.builder("agentic.run.safe_stop").register(meterRegistry).increment();
        Counter.builder("agentic.safe_stops.total").register(meterRegistry).increment();
    }

    @Transactional
    public void rollback(UUID runId, String reason) {
        requireNonBlank(reason, "reason");
        OrchestrationRun run = getRun(runId);
        if (run.getStatus() != RunStatus.COMPLETED && run.getStatus() != RunStatus.RUNNING && run.getStatus() != RunStatus.AWAITING_APPROVAL) {
            throw new InvalidStateException("Rollback not valid in state " + run.getStatus());
        }
        for (OrchestrationNode node : nodeRepository.findByRunIdOrderById(runId)) {
            if (node.getStatus() == NodeStatus.SUCCEEDED &&
                    (node.getNodeType() == NodeType.RELEASE || node.getNodeType() == NodeType.RELEASE_READINESS)) {
                node.markRolledBack(reason);
                nodeRepository.save(node);
            }
        }
        run.setStatus(RunStatus.ROLLED_BACK);
        runRepository.save(run);
        audit(runId, "ROLLBACK", "human", "release", Map.of("reason", reason));
        Counter.builder("agentic.run.rollback").register(meterRegistry).increment();
        Counter.builder("agentic.rollbacks.total").register(meterRegistry).increment();
    }

    @Transactional(readOnly = true)
    public OrchestrationRun getRun(UUID runId) {
        return runRepository.findById(runId).orElseThrow(() -> new NotFoundException("Run does not exist"));
    }

    @Transactional(readOnly = true)
    public List<OrchestrationNode> getNodes(UUID runId) {
        getRun(runId);
        return nodeRepository.findByRunIdOrderById(runId);
    }

    @Transactional(readOnly = true)
    public List<AuditEvent> getAudit(UUID runId) {
        getRun(runId);
        return auditRepository.findByRunIdOrderByEventAtAsc(runId);
    }

    private boolean allSucceeded(List<OrchestrationNode> nodes) {
        return !nodes.isEmpty() && nodes.stream().allMatch(n -> n.getStatus() == NodeStatus.SUCCEEDED);
    }

    private List<OrchestrationNode> readyNodes(List<OrchestrationNode> nodes) {
        return nodes.stream().filter(n -> n.getStatus() == NodeStatus.PENDING && dependenciesSucceeded(n, nodes)
                && n.getNodeType() != NodeType.HUMAN_APPROVAL
                && (isAutonomous(n.getNodeType()) || n.getNodeType() == NodeType.RELEASE)).toList();
    }

    private boolean isAutonomous(NodeType type) {
        return governance.autonomousActions() != null && governance.autonomousActions().contains(type.name());
    }

    private boolean dependenciesSucceeded(OrchestrationNode node, List<OrchestrationNode> nodes) {
        if (node.getDependenciesCsv().isBlank()) return true;
        Set<String> deps = Set.of(node.getDependenciesCsv().split(","));
        return deps.stream().allMatch(dep -> nodes.stream().anyMatch(n -> n.getNodeKey().equals(dep) && n.getStatus() == NodeStatus.SUCCEEDED));
    }

    private void mergeNodeOutput(Map<String, Object> context, String nodeKey, String outputJson) {
        Map<String, Object> outputs = (Map<String, Object>) context.computeIfAbsent("outputs", k -> new LinkedHashMap<>());
        outputs.put(nodeKey, fromJson(outputJson));
        appendDecision(context, nodeKey + " completed and its output became part of shared context");
    }

    private void appendDecision(Map<String, Object> context, String decision) {
        List<String> lineage = (List<String>) context.computeIfAbsent("decisionLineage", k -> new ArrayList<>());
        lineage.add(Instant.now() + " :: " + decision);
    }

    private void saveContext(UUID runId, Map<String, Object> context) {
        OrchestrationRun run = getRun(runId);
        run.setContextJson(toJson(context));
        runRepository.save(run);
    }

    private void audit(UUID runId, String type, String actor, String nodeKey, Map<String, Object> payload) {
        auditRepository.save(new AuditEvent(UUID.randomUUID(), runId, type, actor, nodeKey, Instant.now(), toJson(payload)));
    }

    private void setRunStatus(UUID runId, RunStatus status) {
        OrchestrationRun run = getRun(runId);
        run.setStatus(status);
        runRepository.save(run);
    }

    private void requireToken(String token) {
        if (token == null || !token.equals(governance.approvalToken())) throw new PolicyViolationException("Valid human approval token is required");
    }

    private static void requireNonBlank(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
    }

    private Set<String> downstreamOf(RunScenario scenario, String root) {
        List<WorkflowNodeDefinition> defs = WorkflowTemplates.forScenario(scenario);
        Set<String> selected = new HashSet<>();
        Set<String> frontier = Set.of(root);
        while (!frontier.isEmpty()) {
            Set<String> next = new HashSet<>();
            for (WorkflowNodeDefinition def : defs) {
                if (def.dependencies().stream().anyMatch(frontier::contains) && selected.add(def.key())) next.add(def.key());
            }
            frontier = next;
        }
        return selected;
    }

    private String toJson(Object value) {
        try { return objectMapper.writeValueAsString(value); } catch (JsonProcessingException e) { throw new IllegalStateException(e); }
    }

    private Map<String, Object> fromJson(String value) {
        try { return objectMapper.readValue(value, new TypeReference<LinkedHashMap<String, Object>>() {}); }
        catch (Exception e) { throw new IllegalStateException("Invalid persisted workflow context", e); }
    }

    private record NodeResult(String nodeKey, boolean success, String outputJson, String error, int attempt, boolean waitingForClarification) {}
}
