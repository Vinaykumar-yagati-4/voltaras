package com.voltaras.complaintservice.repository;

import com.voltaras.complaintservice.entity.ComplaintComment;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Comments are loaded through the owning {@code Complaint} aggregate; this
 * repository only exists for direct persistence needs.
 */
public interface ComplaintCommentRepository extends JpaRepository<ComplaintComment, Long> {
}
