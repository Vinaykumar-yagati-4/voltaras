package com.voltaras.organizationservice.repository;

import com.voltaras.organizationservice.entity.Building;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BuildingRepository
        extends JpaRepository<Building, Long> {

    boolean existsByOrganizationIdAndCodeIgnoreCase(Long organizationId, String code);

    Optional<Building> findByIdAndOrganizationId(Long buildingId, Long organizationId);

    List<Building> findAllByOrganizationIdOrderByCreatedAtDesc(Long organizationId);
}
