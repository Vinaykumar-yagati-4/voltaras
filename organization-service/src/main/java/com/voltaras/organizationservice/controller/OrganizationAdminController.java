package com.voltaras.organizationservice.controller;

import com.voltaras.organizationservice.dto.request.CreateMembershipRequest;
import com.voltaras.organizationservice.dto.response.MembershipResponse;
import com.voltaras.organizationservice.dto.response.OrganizationResponse;
import com.voltaras.organizationservice.enums.OrganizationStatus;
import com.voltaras.organizationservice.enums.OrganizationType;
import com.voltaras.organizationservice.service.MembershipService;
import com.voltaras.organizationservice.service.OrganizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/organizations")
@RequiredArgsConstructor
public class OrganizationAdminController {

    private final OrganizationService organizationService;
    private final MembershipService membershipService;

    @GetMapping
    public ResponseEntity<Page<OrganizationResponse>> getAllOrganizationsForAdmin(
            @RequestHeader("X-User-Role") String systemRole,
            @RequestParam(name = "status", required = false) OrganizationStatus status,
            @RequestParam(name = "type", required = false) OrganizationType type,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        Page<OrganizationResponse> response =
                organizationService.getAllOrganizationsForAdmin(
                        systemRole, status, type, pageable);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{organizationId}")
    public ResponseEntity<OrganizationResponse> getOrganizationForAdmin(
            @RequestHeader("X-User-Role") String systemRole,
            @PathVariable Long organizationId) {

        OrganizationResponse response =
                organizationService.getOrganizationForAdmin(systemRole, organizationId);

        return ResponseEntity.ok(response);
    }

    /**
     * Creates (or reactivates) an ACTIVE membership for the given user in
     * the organization. Only MEMBER and MANAGER roles can be assigned here.
     * Requires X-User-Role = ADMIN.
     */
    @PostMapping("/{organizationId}/members")
    public ResponseEntity<MembershipResponse> createMembershipForAdmin(
            @RequestHeader("X-User-Id") Long adminUserId,
            @RequestHeader("X-User-Role") String systemRole,
            @PathVariable Long organizationId,
            @Valid @RequestBody CreateMembershipRequest request) {

        MembershipResponse response =
                membershipService.createMembershipForAdmin(
                        adminUserId, systemRole, organizationId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{organizationId}/suspend")
    public ResponseEntity<OrganizationResponse> suspendOrganizationForAdmin(
            @RequestHeader("X-User-Role") String systemRole,
            @PathVariable Long organizationId) {

        OrganizationResponse response =
                organizationService.suspendOrganizationForAdmin(systemRole, organizationId);

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{organizationId}/activate")
    public ResponseEntity<OrganizationResponse> activateOrganizationForAdmin(
            @RequestHeader("X-User-Role") String systemRole,
            @PathVariable Long organizationId) {

        OrganizationResponse response =
                organizationService.activateOrganizationForAdmin(systemRole, organizationId);

        return ResponseEntity.ok(response);
    }
}
