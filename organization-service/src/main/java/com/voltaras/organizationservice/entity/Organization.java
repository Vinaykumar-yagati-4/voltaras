package com.voltaras.organizationservice.entity;

import com.voltaras.organizationservice.enums.OrganizationStatus;
import com.voltaras.organizationservice.enums.OrganizationType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Represents a hostel, institution, apartment, or commercial organization.
 * <p>
 * Organization membership is optional in VOLTARAS; every user can register
 * and log in normally without belonging to an organization.
 * <p>
 * createdByAuthUserId is the authenticated user ID received from the API
 * Gateway through the X-User-Id header. It is an external Auth Service
 * identity and is intentionally stored as a plain Long without any JPA
 * relationship or foreign key to another microservice database.
 */
@Entity
@Table(
        name = "organizations",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_organization_code",
                columnNames = "organization_code"
        )
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Organization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "organization_code", nullable = false, length = 50)
    private String organizationCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "organization_type", nullable = false, length = 30)
    private OrganizationType organizationType;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "email", length = 150)
    private String email;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "address_line_1", length = 255)
    private String addressLine1;

    @Column(name = "address_line_2", length = 255)
    private String addressLine2;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "state", length = 100)
    private String state;

    @Column(name = "country", length = 100)
    private String country;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    /**
     * External Auth Service user ID received from the API Gateway.
     * No JPA relationship is created to the Auth Service database.
     */
    @Column(name = "created_by_auth_user_id", nullable = false)
    private Long createdByAuthUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OrganizationStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {

        LocalDateTime now = LocalDateTime.now();

        createdAt = now;
        updatedAt = now;

        if (status == null) {
            status = OrganizationStatus.ACTIVE;
        }
    }

    @PreUpdate
    protected void onUpdate() {

        updatedAt = LocalDateTime.now();
    }
}
