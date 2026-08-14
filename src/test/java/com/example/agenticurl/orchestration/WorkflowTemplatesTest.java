package com.example.agenticurl.orchestration;

import com.example.agenticurl.domain.RunScenario;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowTemplatesTest {
    @Test
    void greenfieldHasParallelBranchesThatSynchronizeAtTesting() {
        var defs = WorkflowTemplates.forScenario(RunScenario.GREENFIELD);
        var byKey = defs.stream().collect(Collectors.toMap(WorkflowNodeDefinition::key, x -> x));

        assertThat(byKey.get("implementation").dependencies()).containsExactly("architecture");
        assertThat(byKey.get("documentation").dependencies()).containsExactly("architecture");
        assertThat(byKey.get("testing").dependencies()).containsExactlyInAnyOrder("implementation", "documentation");
        assertThat(byKey.get("human-approval").humanGate()).isTrue();
    }

    @Test
    void scenariosContainDifferentBrownfieldAndAmbiguousReasoningStages() {
        assertThat(WorkflowTemplates.forScenario(RunScenario.BROWNFIELD).stream().map(WorkflowNodeDefinition::type))
                .contains(com.example.agenticurl.domain.NodeType.CODEBASE_REASONING, com.example.agenticurl.domain.NodeType.IMPACT_ANALYSIS);
        assertThat(WorkflowTemplates.forScenario(RunScenario.AMBIGUOUS).stream().map(WorkflowNodeDefinition::type))
                .contains(com.example.agenticurl.domain.NodeType.AMBIGUITY_REVIEW);
    }
}
