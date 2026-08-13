package dev.rheinops.platform.shared.internal;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("rheinops.security.rate-limit")
public record ApiRateLimitProperties(
        boolean enabled,
        @Min(1) long capacity,
        @Min(1) long refillTokens,
        @NotNull Duration refillPeriod,
        @Min(1) long maxClients,
        @NotNull Duration expireAfterAccess) {}
