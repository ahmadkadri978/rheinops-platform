package dev.rheinops.platform.shared.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

class KeycloakRealmRoleConverterTest {

    private final KeycloakRealmRoleConverter converter = new KeycloakRealmRoleConverter();

    @Test
    void mapsOnlyKnownRheinOpsRealmRoles() {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .subject("operator-123")
                .claim(
                        "realm_access",
                        Map.of("roles", List.of(
                                "viewer", "RESPONDER", "default-roles-rheinops", "unknown-role")))
                .build();

        assertThat(converter.convert(jwt))
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_VIEWER", "ROLE_RESPONDER");
    }

    @Test
    void grantsNothingWhenTheRealmRoleClaimIsMissing() {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .subject("operator-123")
                .build();

        assertThat(converter.convert(jwt)).isEmpty();
    }
}
