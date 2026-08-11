package com.voltaras.metermanagementservice.util;

import com.voltaras.metermanagementservice.exception.AccessDeniedException;
import lombok.extern.slf4j.Slf4j;

/**
 * Enforces the ADMIN-only guard on the meter admin APIs.
 *
 * <p>
 * Consistent with the other VOLTARAS services, both {@code ADMIN} and
 * {@code ROLE_ADMIN} spellings of the {@code X-User-Role} header are
 * accepted.
 * </p>
 */
@Slf4j
public final class AdminRoleValidator {

    private static final String ADMIN_ROLE = "ADMIN";
    private static final String ROLE_ADMIN = "ROLE_ADMIN";

    private AdminRoleValidator() {
        // Static utility class; no instances.
    }

    /**
     * Verifies that the role received from {@code X-User-Role} is an admin
     * role.
     *
     * @param systemRole role injected by the API Gateway
     * @throws AccessDeniedException when the role is not ADMIN/ROLE_ADMIN
     */
    public static void requireAdmin(String systemRole) {

        boolean isAdmin = ADMIN_ROLE.equalsIgnoreCase(systemRole)
                || ROLE_ADMIN.equalsIgnoreCase(systemRole);

        if (!isAdmin) {
            log.warn("Forbidden admin operation attempted by role: {}", systemRole);
            throw new AccessDeniedException(
                    "Only ADMIN users can perform this operation");
        }
    }
}
