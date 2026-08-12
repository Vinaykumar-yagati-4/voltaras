package com.voltaras.complaintservice.repository;

import com.voltaras.complaintservice.entity.ComplaintCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ComplaintCategoryRepository extends JpaRepository<ComplaintCategory, Long> {

    List<ComplaintCategory> findAllByActiveTrueOrderByNameAsc();

    Optional<ComplaintCategory> findByIdAndActiveTrue(Long id);

    Optional<ComplaintCategory> findByName(String name);

    boolean existsByName(String name);
}
