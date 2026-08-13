package dev.rheinops.platform.shared.internal;

import dev.rheinops.platform.shared.PlatformRole;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

public class KeycloakRealmRoleConverter
        implements Converter<Jwt, Collection<GrantedAuthority>> {

    private static final Set<String> ALLOWED_ROLES = Set.of(
            PlatformRole.VIEWER.name(),
            PlatformRole.RESPONDER.name(),
            PlatformRole.INCIDENT_MANAGER.name(),
            PlatformRole.ADMINISTRATOR.name());

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess == null || !(realmAccess.get("roles") instanceof Collection<?> roles)) {
            return List.of();
        }

        return roles.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .map(KeycloakRealmRoleConverter::normalise)
                .filter(ALLOWED_ROLES::contains)
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toUnmodifiableSet());
    }

    private static String normalise(String role) {
        return role.trim().replace('-', '_').toUpperCase(Locale.ROOT);
    }
}
