package com.voltaras.complaintservice.security;

import com.voltaras.complaintservice.entity.Complaint;
import com.voltaras.complaintservice.exception.AccessDeniedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Identity and role helpers for the Complaint Service.
 *
 * <p>
 * Identity comes from the API Gateway injected headers {@code X-User-Id}
 * and {@code X-User-Role}. The API Gateway forwards the JWT {@code role}
 * claim verbatim, and the Auth Service issues that claim as the
 * {@code RoleType} enum name ({@code ADMIN}, {@code CONSUMER}) — see
 * {@code JwtTokenProvider.extractRoleFromUser} and
 * {@code JwtAuthenticationFilter} in the gateway. Therefore the canonical
 * admin value is exactly {@code ADMIN}; the {@code ROLE_ADMIN} spelling is
 * never produced by the VOLTARAS gateway and is deliberately not accepted.
 * </p>
 */
@Component
@Slf4j
public class ComplaintAccessHelper {

    private static final String ADMIN_ROLE = "ADMIN";
    private static final String CONSUMER_ROLE = "CONSUMER";

    /**
     * Requires the caller to be a system ADMIN (gateway role format
     * {@code ADMIN}).
     */
    public void requireAdmin(String systemRole) {

        if (!isExactRole(systemRole, ADMIN_ROLE)) {
            log.warn("Forbidden admin operation attempted with role: {}", systemRole);
            throw new AccessDeniedException(
                    "Only ADMIN users can perform this operation");
        }
    }

    /**
     * Requires the caller to be a CONSUMER (gateway role format
     * {@code CONSUMER}). Used for the complaint creation endpoint, which is
     * consumer-only in the documented contract.
     */
    public void requireConsumer(String systemRole) {

        if (!isExactRole(systemRole, CONSUMER_ROLE)) {
            log.warn("Consumer-only operation attempted with role: {}", systemRole);
            throw new AccessDeniedException(
                    "Only CONSUMER users can perform this operation");
        }
    }

    /**
     * Requires the caller to be the complaint owner. Admin users cannot
     * own complaints (creation is consumer-only), so owner-scoped
     * endpoints reject everyone else, admins included; admins use the
     * dedicated admin endpoints instead.
     */
    public void requireOwner(Complaint complaint, Long authUserId) {

        if (authUserId == null || complaint.getConsumerId() == null
                || !complaint.getConsumerId().equals(authUserId)) {

            throw new AccessDeniedException(
                    "You are not allowed to access this complaint");
        }
    }

    private boolean isExactRole(String systemRole, String expected) {

        return systemRole != null && expected.equalsIgnoreCase(systemRole.trim());
    }
}
