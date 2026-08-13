package dev.rheinops.platform.shared;

/**
 * Minimal identity snapshot used for operational audit records.
 *
 * @param subject stable identity-provider subject, never an email address
 * @param displayName operator-facing username captured at the time of the action
 */
public record AuthenticatedActor(String subject, String displayName) {

    public AuthenticatedActor {
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("Actor subject must not be blank.");
        }
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("Actor display name must not be blank.");
        }
    }
}
