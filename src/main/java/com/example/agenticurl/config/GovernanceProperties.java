package com.example.agenticurl.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app.governance")
public record GovernanceProperties(String approvalToken, String controlToken, int maxNodeRetries, int maxRunDurationSeconds,
                                   List<String> autonomousActions) {
}
