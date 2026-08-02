package com.voltaras.organizationservice.repository;

import com.voltaras.organizationservice.entity.Block;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BlockRepository
        extends JpaRepository<Block, Long> {

    boolean existsByBuildingIdAndCodeIgnoreCase(Long buildingId, String code);

    Optional<Block> findByIdAndBuildingId(Long blockId, Long buildingId);

    List<Block> findAllByBuildingIdOrderByCreatedAtDesc(Long buildingId);

    boolean existsByBuildingId(Long buildingId);
}
