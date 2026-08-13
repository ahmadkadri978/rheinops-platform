package dev.rheinops.platform;

import static org.assertj.core.api.Assertions.assertThat;

import dev.rheinops.platform.shared.internal.ApiErrorHandler;
import dev.rheinops.platform.shared.internal.CorrelationIdFilter;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ArchitectureTest {

    @Test
    void modulesRespectDeclaredBoundaries() {
        ApplicationModules.of(RheinOpsPlatformApplication.class).verify();
    }

    @Test
    void technicalSharedComponentsStayBehindTheModuleApi() {
        var shared = ApplicationModules.of(RheinOpsPlatformApplication.class)
                .getModuleByName("shared")
                .orElseThrow();

        assertThat(shared.isExposed(ApiErrorHandler.class)).isFalse();
        assertThat(shared.isExposed(CorrelationIdFilter.class)).isFalse();
    }
}
