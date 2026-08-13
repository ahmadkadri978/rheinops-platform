/**
 * Owns customer support cases, SLA policies, diagnostics, escalation, and
 * operational read models.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Support Operations",
        allowedDependencies = {"incident", "servicecatalog", "shared"})
package dev.rheinops.platform.support;
