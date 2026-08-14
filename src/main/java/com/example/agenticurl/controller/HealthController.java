package com.example.agenticurl.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
public class HealthController {
    @GetMapping("/api/v1/demo")
    public Map<String, Object> demo() {
        return Map.of(
                "service", "agentic-url-shortener",
                "status", "ready",
                "timestamp", Instant.now().toString(),
                "purpose", "Governed agentic SDLC orchestration prototype"
        );
    }
}
