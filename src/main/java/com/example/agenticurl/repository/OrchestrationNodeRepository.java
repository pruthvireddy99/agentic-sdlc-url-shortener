package com.example.agenticurl.repository;

import com.example.agenticurl.domain.OrchestrationNode;
import com.example.agenticurl.domain.NodeStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrchestrationNodeRepository extends JpaRepository<OrchestrationNode, UUID> {
    List<OrchestrationNode> findByRunIdOrderById(UUID runId);
    List<OrchestrationNode> findByRunIdAndStatus(UUID runId, NodeStatus status);
}
