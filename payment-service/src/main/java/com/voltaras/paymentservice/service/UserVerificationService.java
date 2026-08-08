package com.voltaras.paymentservice.service;

/**
 * Verifies the authenticated user against the Auth Service before
 * recharge and wallet bill payment operations are allowed.
 */
public interface UserVerificationService {

    /**
     * Verifies with the Auth Service that the authenticated user exists,
     * is active, and that its identity (user ID, role, email) matches the
     * gateway-injected headers.
     *
     * <p>
     * Throws (mapped to the standardized error envelope):</p>
     *
     * <ul>
     *     <li>{@code USER_NOT_FOUND} when the user does not exist</li>
     *     <li>{@code USER_INACTIVE} when the user is deactivated</li>
     *     <li>{@code UNAUTHORIZED_USER} when the identity cannot be
     *         verified (token rejected, user ID or role mismatch)</li>
     *     <li>{@code UPSTREAM_SERVICE_ERROR} when the Auth Service is
     *         unreachable</li>
     * </ul>
     *
     * @param authUserId user ID from X-User-Id
     * @param systemRole role from X-User-Role
     */
    void verifyActiveUser(Long authUserId, String systemRole);
}
