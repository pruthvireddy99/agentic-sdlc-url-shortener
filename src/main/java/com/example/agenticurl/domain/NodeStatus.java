package com.example.agenticurl.domain;

public enum NodeStatus {
    PENDING,
    RUNNING,
    SUCCEEDED,
    FAILED,
    AWAITING_APPROVAL,
    AWAITING_CLARIFICATION,
    BLOCKED,
    ROLLED_BACK,
    SKIPPED
}
