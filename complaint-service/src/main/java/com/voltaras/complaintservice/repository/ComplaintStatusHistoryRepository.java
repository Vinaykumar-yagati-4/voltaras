package com.voltaras.complaintservice.repository;

import com.voltaras.complaintservice.entity.ComplaintStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Status history is loaded through the owning {@code Complaint} aggregate;
 * this repository only exists for direct persistence needs.
 */
public interface ComplaintStatusHistoryRepository extends JpaRepository<ComplaintStatusHistory, Long> {
}
