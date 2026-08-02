package com.voltaras.organizationservice.repository;

import com.voltaras.organizationservice.entity.Organization;
import com.voltaras.organizationservice.enums.OrganizationStatus;
import com.voltaras.organizationservice.enums.OrganizationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrganizationRepository
        extends JpaRepository<Organization, Long> {

    boolean existsByOrganizationCodeIgnoreCase(String organizationCode);

    Optional<Organization> findByOrganizationCodeIgnoreCase(String organizationCode);

    List<Organization> findAllByCreatedByAuthUserId(Long authUserId);

    List<Organization> findAllByStatusOrderByCreatedAtDesc(OrganizationStatus status);

    List<Organization> findAllByOrderByCreatedAtDesc();

    // ---- Paginated queries for the system-admin dashboard ----

    Page<Organization> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<Organization> findAllByStatusOrderByCreatedAtDesc(
            OrganizationStatus status, Pageable pageable);

    Page<Organization> findAllByOrganizationTypeOrderByCreatedAtDesc(
            OrganizationType type, Pageable pageable);

    Page<Organization> findAllByStatusAndOrganizationTypeOrderByCreatedAtDesc(
            OrganizationStatus status, OrganizationType type, Pageable pageable);
}
