package com.example.agenticurl.service;

import com.example.agenticurl.config.ShortUrlProperties;
import com.example.agenticurl.domain.ShortUrl;
import com.example.agenticurl.repository.ClickEventRepository;
import com.example.agenticurl.repository.ShortUrlRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class UrlServiceTest {
    private final ShortUrlRepository urls = Mockito.mock(ShortUrlRepository.class);
    private final ClickEventRepository clicks = Mockito.mock(ClickEventRepository.class);
    private final UrlService service = new UrlService(urls, clicks,
            new ShortUrlProperties("http://localhost:8080", 7, 2048), new SimpleMeterRegistry());

    @Test
    void idempotencyReturnsExistingResult() {
        ShortUrl existing = new ShortUrl(java.util.UUID.randomUUID(), "AbC1234", "https://example.com", java.time.Instant.now(), null, "same-key");
        when(urls.findByIdempotencyKey("same-key")).thenReturn(Optional.of(existing));

        var result = service.shorten("https://example.com", "same-key", null);

        assertThat(result.code()).isEqualTo("AbC1234");
        assertThat(result.shortUrl()).isEqualTo("http://localhost:8080/AbC1234");
    }

    @Test
    void unsupportedSchemeIsRejected() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.shorten("ftp://example.com", "k", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Only http and https");
    }
}
