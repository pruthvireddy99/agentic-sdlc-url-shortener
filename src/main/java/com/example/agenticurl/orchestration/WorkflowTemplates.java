package com.example.agenticurl.orchestration;

import com.example.agenticurl.domain.NodeType;
import com.example.agenticurl.domain.RunScenario;

import java.util.ArrayList;
import java.util.List;

public final class WorkflowTemplates {
    private WorkflowTemplates() {}

    public static List<WorkflowNodeDefinition> forScenario(RunScenario scenario) {
        return switch (scenario) {
            case GREENFIELD -> List.of(
                    n("requirements", NodeType.REQUIREMENTS),
                    n("architecture", NodeType.ARCHITECTURE, "requirements"),
                    n("implementation", NodeType.IMPLEMENTATION, "architecture"),
                    n("documentation", NodeType.DOCUMENTATION, "architecture"),
                    n("testing", NodeType.TESTING, "implementation", "documentation"),
                    n("release-readiness", NodeType.RELEASE_READINESS, "testing"),
                    gate("human-approval", NodeType.HUMAN_APPROVAL, "release-readiness"),
                    n("release", NodeType.RELEASE, "human-approval")
            );
            case BROWNFIELD -> List.of(
                    n("requirements", NodeType.REQUIREMENTS),
                    n("codebase-reasoning", NodeType.CODEBASE_REASONING, "requirements"),
                    n("impact-analysis", NodeType.IMPACT_ANALYSIS, "codebase-reasoning"),
                    n("architecture", NodeType.ARCHITECTURE, "impact-analysis"),
                    n("implementation", NodeType.IMPLEMENTATION, "architecture"),
                    n("documentation", NodeType.DOCUMENTATION, "architecture"),
                    n("testing", NodeType.TESTING, "implementation", "documentation"),
                    n("release-readiness", NodeType.RELEASE_READINESS, "testing"),
                    gate("human-approval", NodeType.HUMAN_APPROVAL, "release-readiness"),
                    n("release", NodeType.RELEASE, "human-approval")
            );
            case AMBIGUOUS -> List.of(
                    n("requirements", NodeType.REQUIREMENTS),
                    n("ambiguity-review", NodeType.AMBIGUITY_REVIEW, "requirements"),
                    n("architecture", NodeType.ARCHITECTURE, "ambiguity-review"),
                    n("implementation", NodeType.IMPLEMENTATION, "architecture"),
                    n("documentation", NodeType.DOCUMENTATION, "architecture"),
                    n("testing", NodeType.TESTING, "implementation", "documentation"),
                    n("release-readiness", NodeType.RELEASE_READINESS, "testing"),
                    gate("human-approval", NodeType.HUMAN_APPROVAL, "release-readiness"),
                    n("release", NodeType.RELEASE, "human-approval")
            );
        };
    }

    private static WorkflowNodeDefinition n(String key, NodeType type, String... dependencies) {
        return new WorkflowNodeDefinition(key, type, List.of(dependencies), false);
    }

    private static WorkflowNodeDefinition gate(String key, NodeType type, String... dependencies) {
        return new WorkflowNodeDefinition(key, type, List.of(dependencies), true);
    }
}
