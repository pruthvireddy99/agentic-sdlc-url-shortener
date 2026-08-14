package com.example.agenticurl.orchestration;

import com.example.agenticurl.domain.NodeType;

import java.util.List;

public record WorkflowNodeDefinition(String key, NodeType type, List<String> dependencies, boolean humanGate) {
}
