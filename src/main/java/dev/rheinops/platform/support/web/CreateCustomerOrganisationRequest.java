package dev.rheinops.platform.support.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

record CreateCustomerOrganisationRequest(
        @NotBlank @Size(max = 160) String name,
        @NotBlank @Size(max = 80)
                @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "must be a URL-safe slug")
                String slug) {}
