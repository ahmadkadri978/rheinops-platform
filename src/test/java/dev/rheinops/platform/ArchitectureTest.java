package dev.rheinops.platform;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ArchitectureTest {

    @Test
    void modulesRespectDeclaredBoundaries() {
        ApplicationModules.of(RheinOpsPlatformApplication.class).verify();
    }
}
