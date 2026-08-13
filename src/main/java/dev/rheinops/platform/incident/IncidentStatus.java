package dev.rheinops.platform.incident;

import java.util.EnumSet;
import java.util.Set;

public enum IncidentStatus {
    OPEN,
    INVESTIGATING,
    IDENTIFIED,
    MONITORING,
    RESOLVED;

    private static final Set<IncidentStatus> TERMINAL = EnumSet.of(RESOLVED);

    public boolean canTransitionTo(IncidentStatus next) {
        if (this == next) {
            return true;
        }
        if (TERMINAL.contains(this)) {
            return false;
        }
        return switch (this) {
            case OPEN -> next == INVESTIGATING || next == RESOLVED;
            case INVESTIGATING -> next == IDENTIFIED || next == MONITORING || next == RESOLVED;
            case IDENTIFIED -> next == MONITORING || next == RESOLVED;
            case MONITORING -> next == RESOLVED || next == INVESTIGATING;
            case RESOLVED -> false;
        };
    }
}
