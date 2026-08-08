package com.voltaras.paymentservice.security;

import com.voltaras.paymentservice.entity.PaymentTransaction;
import com.voltaras.paymentservice.exception.BadRequestException;
import com.voltaras.paymentservice.exception.ForbiddenOperationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Identity and role helpers for the Payment Service.
 *
 * <p>
 * Identity comes from the API Gateway injected headers X-User-Id and
 * X-User-Role. Admin role validation is consistent with the other VOLTARAS
 * services: both {@code ADMIN} and {@code ROLE_ADMIN} are accepted.
 * </p>
 */
@Component
@Slf4j
public class PaymentAccessHelper {

    private static final String SYSTEM_ADMIN_ROLE = "ADMIN";

    /**
     * Requires a valid authenticated user ID.
     *
     * @param authUserId authenticated user ID from X-User-Id
     */
    public void requireAuthenticatedUser(Long authUserId) {

        if (authUserId == null) {
            throw new BadRequestException(
                    "Authenticated user is required"
            );
        }
    }

    /**
     * Checks whether the provided platform role is a system ADMIN role.
     *
     * @param systemRole role received from X-User-Role
     * @return true when role is ADMIN or ROLE_ADMIN
     */
    public boolean isSystemAdmin(String systemRole) {

        if (systemRole == null) {
            return false;
        }

        String normalized =
                systemRole.trim().toUpperCase();

        if (normalized.startsWith("ROLE_")) {
            normalized =
                    normalized.substring("ROLE_".length());
        }

        return SYSTEM_ADMIN_ROLE.equals(normalized);
    }

    /**
     * Requires the caller to be a system ADMIN.
     *
     * @param systemRole role received from API Gateway
     */
    public void requireSystemAdmin(String systemRole) {

        if (!isSystemAdmin(systemRole)) {
            throw new ForbiddenOperationException(
                    "Only system ADMIN users can perform this operation"
            );
        }
    }

    /**
     * Requires the caller to own the payment or to be a system ADMIN.
     *
     * @param payment the payment transaction being accessed
     * @param authUserId authenticated user ID
     * @param systemRole platform role
     */
    public void requirePaymentAccess(
            PaymentTransaction payment, Long authUserId, String systemRole) {

        requireAuthenticatedUser(authUserId);

        if (payment.getUserId().equals(authUserId)) {
            return;
        }

        if (isSystemAdmin(systemRole)) {
            return;
        }

        throw new ForbiddenOperationException(
                "You are not allowed to access this payment"
        );
    }
}
