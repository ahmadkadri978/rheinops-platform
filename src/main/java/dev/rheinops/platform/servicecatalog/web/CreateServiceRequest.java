package dev.rheinops.platform.servicecatalog.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

record CreateServiceRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank
                @Size(max = 80)
                @Pattern(
                        regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$",
                        message = "must contain lowercase letters, numbers, and single hyphens only")
                String slug,
        @Size(max = 500) String description) {}
