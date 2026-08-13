package dev.rheinops.platform.support;

public enum SupportCaseStatus {
    OPEN,
    IN_PROGRESS,
    WAITING_FOR_CUSTOMER,
    RESOLVED,
    CLOSED;

    public boolean canTransitionTo(SupportCaseStatus next) {
        if (this == next) {
            return true;
        }
        return switch (this) {
            case OPEN -> next == IN_PROGRESS || next == WAITING_FOR_CUSTOMER || next == RESOLVED;
            case IN_PROGRESS -> next == WAITING_FOR_CUSTOMER || next == RESOLVED;
            case WAITING_FOR_CUSTOMER -> next == IN_PROGRESS || next == RESOLVED;
            case RESOLVED -> next == CLOSED;
            case CLOSED -> false;
        };
    }

    public boolean isTerminal() {
        return this == CLOSED;
    }
}
