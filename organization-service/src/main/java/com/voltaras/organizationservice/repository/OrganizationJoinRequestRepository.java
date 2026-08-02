package com.voltaras.organizationservice.repository;

import com.voltaras.organizationservice.entity.OrganizationJoinRequest;
import com.voltaras.organizationservice.enums.JoinRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrganizationJoinRequestRepository
        extends JpaRepository<OrganizationJoinRequest, Long> {

    boolean existsByOrganizationIdAndAuthUserIdAndStatus(
            Long organizationId, Long authUserId, JoinRequestStatus status);

    Optional<OrganizationJoinRequest> findByIdAndOrganizationId(
            Long requestId, Long organizationId);

    Optional<OrganizationJoinRequest> findByOrganizationIdAndAuthUserIdAndStatus(
            Long organizationId, Long authUserId, JoinRequestStatus status);

    List<OrganizationJoinRequest> findAllByOrganizationIdOrderByCreatedAtDesc(
            Long organizationId);

    List<OrganizationJoinRequest> findAllByOrganizationIdAndStatusOrderByCreatedAtDesc(
            Long organizationId, JoinRequestStatus status);

    List<OrganizationJoinRequest> findAllByAuthUserIdOrderByCreatedAtDesc(
            Long authUserId);
}
