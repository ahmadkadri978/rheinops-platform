package dev.rheinops.platform.shared.internal;

import dev.rheinops.platform.shared.ActorProvider;
import dev.rheinops.platform.shared.AuthenticatedActor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
class SecurityContextActorProvider implements ActorProvider {

    @Override
    public AuthenticatedActor currentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)
                || !authentication.isAuthenticated()) {
            throw new IllegalStateException("An authenticated JWT actor is required.");
        }

        String subject = jwtAuthentication.getToken().getSubject();
        String preferredUsername = jwtAuthentication.getToken().getClaimAsString("preferred_username");
        String displayName = preferredUsername == null || preferredUsername.isBlank()
                ? subject
                : preferredUsername;
        return new AuthenticatedActor(subject, displayName);
    }
}
