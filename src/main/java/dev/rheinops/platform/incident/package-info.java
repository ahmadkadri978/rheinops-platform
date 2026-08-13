/**
 * Owns incident lifecycle management and operational prioritisation.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Incident Management",
        allowedDependencies = {"servicecatalog", "shared"})
package dev.rheinops.platform.incident;
