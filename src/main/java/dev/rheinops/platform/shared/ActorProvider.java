package dev.rheinops.platform.shared;

/**
 * Provides the authenticated actor without coupling business modules to Spring Security.
 */
public interface ActorProvider {

    AuthenticatedActor currentActor();
}
