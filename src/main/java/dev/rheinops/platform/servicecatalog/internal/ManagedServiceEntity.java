package dev.rheinops.platform.servicecatalog.internal;

import dev.rheinops.platform.servicecatalog.ServiceStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "managed_services")
class ManagedServiceEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, unique = true, length = 80)
    private String slug;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ServiceStatus status;

    @Version
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ManagedServiceEntity() {}

    static ManagedServiceEntity create(
            UUID id, String name, String slug, String description, Instant now) {
        ManagedServiceEntity service = new ManagedServiceEntity();
        service.id = id;
        service.name = name;
        service.slug = slug;
        service.description = description;
        service.status = ServiceStatus.OPERATIONAL;
        service.createdAt = now;
        service.updatedAt = now;
        return service;
    }

    UUID id() {
        return id;
    }

    String name() {
        return name;
    }

    String slug() {
        return slug;
    }

    String description() {
        return description;
    }

    ServiceStatus status() {
        return status;
    }

    long version() {
        return version;
    }

    Instant createdAt() {
        return createdAt;
    }

    Instant updatedAt() {
        return updatedAt;
    }
}
