package com.voltaras.organizationservice.controller;

import com.voltaras.organizationservice.dto.request.CreateJoinRequest;
import com.voltaras.organizationservice.dto.request.RejectJoinRequest;
import com.voltaras.organizationservice.dto.response.JoinRequestResponse;
import com.voltaras.organizationservice.enums.JoinRequestStatus;
import com.voltaras.organizationservice.service.JoinRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/organizations")
@RequiredArgsConstructor
public class JoinRequestController {

    private final JoinRequestService joinRequestService;

    @PostMapping("/{organizationId}/join-requests")
    public ResponseEntity<JoinRequestResponse> createJoinRequest(
            @RequestHeader("X-User-Id") Long authUserId,
            @PathVariable Long organizationId,
            @Valid @RequestBody CreateJoinRequest request) {

        JoinRequestResponse response =
                joinRequestService.createJoinRequest(authUserId, organizationId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{organizationId}/join-requests")
    public ResponseEntity<List<JoinRequestResponse>> getOrganizationJoinRequests(
            @RequestHeader("X-User-Id") Long adminUserId,
            @PathVariable Long organizationId,
            @RequestParam(name = "status", required = false) JoinRequestStatus status) {

        List<JoinRequestResponse> response =
                joinRequestService.getOrganizationJoinRequests(
                        adminUserId, organizationId, status);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/join-requests/me")
    public ResponseEntity<List<JoinRequestResponse>> getMyJoinRequests(
            @RequestHeader("X-User-Id") Long authUserId) {

        List<JoinRequestResponse> response =
                joinRequestService.getMyJoinRequests(authUserId);

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{organizationId}/join-requests/{requestId}/approve")
    public ResponseEntity<JoinRequestResponse> approveJoinRequest(
            @RequestHeader("X-User-Id") Long reviewerUserId,
            @PathVariable Long organizationId,
            @PathVariable Long requestId) {

        JoinRequestResponse response =
                joinRequestService.approveJoinRequest(
                        reviewerUserId, organizationId, requestId);

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{organizationId}/join-requests/{requestId}/reject")
    public ResponseEntity<JoinRequestResponse> rejectJoinRequest(
            @RequestHeader("X-User-Id") Long reviewerUserId,
            @PathVariable Long organizationId,
            @PathVariable Long requestId,
            @Valid @RequestBody RejectJoinRequest request) {

        JoinRequestResponse response =
                joinRequestService.rejectJoinRequest(
                        reviewerUserId, organizationId, requestId, request);

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{organizationId}/join-requests/{requestId}/cancel")
    public ResponseEntity<JoinRequestResponse> cancelJoinRequest(
            @RequestHeader("X-User-Id") Long authUserId,
            @PathVariable Long organizationId,
            @PathVariable Long requestId) {

        JoinRequestResponse response =
                joinRequestService.cancelJoinRequest(
                        authUserId, organizationId, requestId);

        return ResponseEntity.ok(response);
    }
}
