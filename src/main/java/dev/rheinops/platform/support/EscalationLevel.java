package dev.rheinops.platform.support;

public enum EscalationLevel {
    NONE,
    WARNING,
    BREACHED;

    public static EscalationLevel requiredFor(SlaHealth health) {
        return switch (health) {
            case AT_RISK -> WARNING;
            case BREACHED -> BREACHED;
            case ON_TRACK, MET -> NONE;
        };
    }
}
