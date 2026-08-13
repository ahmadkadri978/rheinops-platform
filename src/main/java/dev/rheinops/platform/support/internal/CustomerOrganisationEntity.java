package dev.rheinops.platform.support.internal;

import dev.rheinops.platform.support.CustomerOrganisationStatus;
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
@Table(name = "customer_organisations")
class CustomerOrganisationEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(nullable = false, unique = true, length = 80)
    private String slug;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private CustomerOrganisationStatus status;

    @Version
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CustomerOrganisationEntity() {}

    static CustomerOrganisationEntity create(UUID id, String name, String slug, Instant now) {
        CustomerOrganisationEntity organisation = new CustomerOrganisationEntity();
        organisation.id = id;
        organisation.name = name;
        organisation.slug = slug;
        organisation.status = CustomerOrganisationStatus.ACTIVE;
        organisation.createdAt = now;
        organisation.updatedAt = now;
        return organisation;
    }

    UUID id() { return id; }
    String name() { return name; }
    String slug() { return slug; }
    CustomerOrganisationStatus status() { return status; }
    long version() { return version; }
    Instant createdAt() { return createdAt; }
    Instant updatedAt() { return updatedAt; }
}
