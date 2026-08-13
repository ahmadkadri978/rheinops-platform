package dev.rheinops.platform.shared;

public enum PlatformRole {
    VIEWER,
    RESPONDER,
    INCIDENT_MANAGER,
    ADMINISTRATOR;

    public String authority() {
        return "ROLE_" + name();
    }
}
