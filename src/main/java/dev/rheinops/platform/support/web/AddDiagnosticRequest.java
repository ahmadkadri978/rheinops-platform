package dev.rheinops.platform.support.web;

import dev.rheinops.platform.support.DiagnosticType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

record AddDiagnosticRequest(
        @NotNull DiagnosticType type,
        @NotBlank @Size(max = 240) String summary,
        @NotBlank @Size(max = 8000) String details) {}
