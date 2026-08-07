package com.voltaras.organizationservice.controller;

import com.voltaras.organizationservice.dto.request.UpdateMembershipRoleRequest;
import com.voltaras.organizationservice.dto.response.MembershipResponse;
import com.voltaras.organizationservice.dto.response.MessageResponse;
import com.voltaras.organizationservice.service.MembershipService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/organizations")
@RequiredArgsConstructor
public class MembershipController {

    private final MembershipService membershipService;

    @GetMapping("/{organizationId}/members")
    public ResponseEntity<Page<MembershipResponse>> getOrganizationMembers(
            @RequestHeader("X-User-Id") Long requesterUserId,
            @PathVariable Long organizationId,
            @ParameterObject
            @PageableDefault(
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            )
            Pageable pageable
    ) {

        Page<MembershipResponse> response =
                membershipService.getOrganizationMembers(
                        requesterUserId,
                        organizationId,
                        pageable
                );

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{organizationId}/members/{membershipId}/role")
    public ResponseEntity<MembershipResponse> updateMembershipRole(
            @RequestHeader("X-User-Id") Long requesterUserId,
            @PathVariable Long organizationId,
            @PathVariable Long membershipId,
            @Valid @RequestBody UpdateMembershipRoleRequest request
    ) {

        MembershipResponse response =
                membershipService.updateMembershipRole(
                        requesterUserId,
                        organizationId,
                        membershipId,
                        request
                );

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{organizationId}/members/{membershipId}/suspend")
    public ResponseEntity<MembershipResponse> suspendMember(
            @RequestHeader("X-User-Id") Long requesterUserId,
            @PathVariable Long organizationId,
            @PathVariable Long membershipId
    ) {

        MembershipResponse response =
                membershipService.suspendMember(
                        requesterUserId,
                        organizationId,
                        membershipId
                );

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{organizationId}/members/{membershipId}")
    public ResponseEntity<MessageResponse> removeMember(
            @RequestHeader("X-User-Id") Long requesterUserId,
            @PathVariable Long organizationId,
            @PathVariable Long membershipId
    ) {

        MessageResponse response =
                membershipService.removeMember(
                        requesterUserId,
                        organizationId,
                        membershipId
                );

        return ResponseEntity.ok(response);
    }
}