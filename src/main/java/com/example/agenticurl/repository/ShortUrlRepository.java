package com.example.agenticurl.repository;

import com.example.agenticurl.domain.ShortUrl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface ShortUrlRepository extends JpaRepository<ShortUrl, UUID> {
    Optional<ShortUrl> findByShortCode(String shortCode);
    Optional<ShortUrl> findByIdempotencyKey(String idempotencyKey);

    @Modifying
    @Query("update ShortUrl s set s.clickCount = s.clickCount + 1 where s.shortCode = :shortCode")
    int incrementClickCount(String shortCode);
}
