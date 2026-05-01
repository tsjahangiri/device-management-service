package com.device.management.device_service.domain;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;


@Entity
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@Table(name = "devices")
public class DeviceEntity {

    /**
     * Hybrid identifier strategy — two IDs serve distinct purposes:
     *
     * - {@code id} (Long): the internal primary key, used exclusively for database
     *   operations such as joins, indexing, and sequence generation. A numeric sequence
     *   ensures ordered, cache-friendly inserts and allows Hibernate to batch operations
     *   efficiently. This field is never exposed outside the persistence layer.
     *
     * - {@code deviceId} (UUID): the public-facing identifier exposed through the API.
     *   Decoupling the external identity from the internal primary key prevents sequential
     *   ID enumeration attacks, hides the internal data model, and ensures the public
     *   contract remains stable regardless of any internal schema changes.
     */
    @Id
    @Column(nullable = false, updatable = false)
    @SequenceGenerator(
            name = "device_sequence",
            sequenceName = "device_sequence",
            allocationSize = 1,
            initialValue = 10000
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "device_sequence"
    )
    private Long id;

    @Column(nullable = false, updatable = false, unique = true)
    private UUID deviceId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String brand;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private State state;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private OffsetDateTime dateCreated;

    @LastModifiedDate
    @Column(nullable = false)
    private OffsetDateTime lastUpdated;

    @PrePersist
    protected void onCreate() {
        this.deviceId = UUID.randomUUID();
    }
}
