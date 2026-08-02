package com.voltaras.organizationservice.repository;

import com.voltaras.organizationservice.entity.Unit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UnitRepository
        extends JpaRepository<Unit, Long> {

    boolean existsByFloorIdAndUnitNumberIgnoreCase(Long floorId, String unitNumber);

    Optional<Unit> findByIdAndFloorId(Long unitId, Long floorId);

    List<Unit> findAllByFloorIdOrderByUnitNumberAsc(Long floorId);

    boolean existsByFloorId(Long floorId);
}
