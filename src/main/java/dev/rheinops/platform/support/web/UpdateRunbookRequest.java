package dev.rheinops.platform.support.web;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

record UpdateRunbookRequest(
        @Size(max = 500)
        @Pattern(regexp = "^(?:|https://.*)$", message = "must use HTTPS")
                String runbookUrl,
        @NotNull @PositiveOrZero Long expectedVersion) {}
