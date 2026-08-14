package com.example.agenticurl.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.short-url")
public record ShortUrlProperties(String publicBaseUrl, int codeLength, int maxOriginalUrlLength) {
}
