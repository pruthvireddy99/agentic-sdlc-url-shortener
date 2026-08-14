package com.example.agenticurl.repository;

import com.example.agenticurl.domain.OrchestrationRun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OrchestrationRunRepository extends JpaRepository<OrchestrationRun, UUID> {
}
