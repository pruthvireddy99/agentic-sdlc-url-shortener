package com.example.agenticurl.repository;

import com.example.agenticurl.domain.ClickEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.UUID;

public interface ClickEventRepository extends JpaRepository<ClickEvent, UUID> {
    long countByShortCode(String shortCode);

    @Query("select count(c) from ClickEvent c where c.shortCode = :shortCode and c.occurredAt >= :from")
    long countSince(String shortCode, Instant from);
}
