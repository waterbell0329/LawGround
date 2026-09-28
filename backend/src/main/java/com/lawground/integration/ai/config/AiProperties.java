package com.lawground.integration.ai.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Internal API configuration; operation-specific clients will be implemented in Y08. */
@Validated
@ConfigurationProperties("lawground.ai")
public record AiProperties(
        @NotNull URI baseUrl,
        String apiKey,
        @NotNull Duration connectTimeout,
        @Valid @NotNull Timeouts timeouts) {
    public record Timeouts(
            @NotNull Duration embed,
            @NotNull Duration tokenize,
            @NotNull Duration hyde,
            @NotNull Duration rerank,
            @NotNull Duration verify,
            @NotNull Duration answer) {}
}
