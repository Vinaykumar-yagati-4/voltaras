package com.voltaras.organizationservice.repository;

import com.voltaras.organizationservice.entity.Floor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FloorRepository
        extends JpaRepository<Floor, Long> {

    boolean existsByBlockIdAndFloorNumber(Long blockId, Integer floorNumber);

    Optional<Floor> findByIdAndBlockId(Long floorId, Long blockId);

    List<Floor> findAllByBlockIdOrderByFloorNumberAsc(Long blockId);

    boolean existsByBlockId(Long blockId);
}
