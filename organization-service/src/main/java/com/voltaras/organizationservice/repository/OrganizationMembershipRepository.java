package com.voltaras.organizationservice.repository;

import com.voltaras.organizationservice.entity.OrganizationMembership;
import com.voltaras.organizationservice.enums.MembershipRole;
import com.voltaras.organizationservice.enums.MembershipStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrganizationMembershipRepository
        extends JpaRepository<OrganizationMembership, Long> {

    boolean existsByOrganizationIdAndAuthUserId(Long organizationId, Long authUserId);

    boolean existsByOrganizationIdAndAuthUserIdAndMembershipStatus(
            Long organizationId, Long authUserId, MembershipStatus membershipStatus);

    Optional<OrganizationMembership> findByOrganizationIdAndAuthUserId(
            Long organizationId, Long authUserId);

    Optional<OrganizationMembership> findByIdAndOrganizationId(
            Long membershipId, Long organizationId);

    List<OrganizationMembership> findAllByOrganizationIdOrderByCreatedAtDesc(
            Long organizationId);

    List<OrganizationMembership> findAllByAuthUserIdOrderByCreatedAtDesc(
            Long authUserId);

    List<OrganizationMembership> findAllByAuthUserIdAndMembershipStatusOrderByCreatedAtDesc(
            Long authUserId, MembershipStatus membershipStatus);

    long countByOrganizationIdAndMembershipRoleAndMembershipStatus(
            Long organizationId, MembershipRole membershipRole, MembershipStatus membershipStatus);

    Page<OrganizationMembership> findAllByOrganizationIdOrderByCreatedAtDesc(
            Long organizationId, Pageable pageable);
}
