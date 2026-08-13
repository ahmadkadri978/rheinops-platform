package dev.rheinops.platform.shared.internal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("rheinops.security.jwt")
public record JwtSecurityProperties(
        @NotNull URI issuerUri,
        @NotNull URI jwkSetUri,
        @NotBlank String audience) {}
