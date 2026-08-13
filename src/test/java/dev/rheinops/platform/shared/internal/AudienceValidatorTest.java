package dev.rheinops.platform.shared.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class AudienceValidatorTest {

    private final AudienceValidator validator = new AudienceValidator("rheinops-api");

    @Test
    void acceptsTheRheinOpsAudience() {
        assertThat(validator.validate(jwtWithAudience("rheinops-api")).hasErrors()).isFalse();
    }

    @Test
    void rejectsATokenIntendedForAnotherApi() {
        assertThat(validator.validate(jwtWithAudience("different-api")).hasErrors()).isTrue();
    }

    private static Jwt jwtWithAudience(String audience) {
        return Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .subject("operator-123")
                .audience(List.of(audience))
                .build();
    }
}
