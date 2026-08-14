package com.example.agenticurl.orchestration;

import com.example.agenticurl.domain.NodeStatus;
import com.example.agenticurl.domain.RunScenario;
import com.example.agenticurl.domain.RunStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class OrchestrationServiceTest {
    @Autowired OrchestrationService service;

    @Test
    void ambiguousScenarioStopsForHumanClarificationThenApproval() {
        var id = service.createRun(RunScenario.AMBIGUOUS,
                "Build a URL shortener with analytics and safe production delivery.", null);

        service.execute(id);
        assertThat(service.getRun(id).getStatus()).isEqualTo(RunStatus.AWAITING_CLARIFICATION);

        service.clarify(id, "Use 90-day analytics retention and treat the LLM provider as an optional adapter.");
        service.execute(id);
        assertThat(service.getRun(id).getStatus()).isEqualTo(RunStatus.AWAITING_APPROVAL);

        service.approve(id, "test-token", "approved for prototype release");
        service.execute(id);
        assertThat(service.getRun(id).getStatus()).isEqualTo(RunStatus.COMPLETED);
        assertThat(service.getNodes(id)).allMatch(n -> n.getStatus() == NodeStatus.SUCCEEDED);
    }
}
