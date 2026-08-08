package com.voltaras.paymentservice.service.impl;

import com.voltaras.paymentservice.client.AuthServiceClient;
import com.voltaras.paymentservice.dto.response.AuthUserResponse;
import com.voltaras.paymentservice.exception.InactiveUserException;
import com.voltaras.paymentservice.exception.UnauthorizedUserException;
import com.voltaras.paymentservice.exception.UpstreamServiceException;
import com.voltaras.paymentservice.exception.UserNotFoundException;
import com.voltaras.paymentservice.service.UserVerificationService;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Implementation of {@link UserVerificationService} backed by the
 * {@link AuthServiceClient} Feign client.
 *
 * <p>
 * The Payment Service never reads the Auth Service database: user
 * existence, email, full name, role and active status are all verified
 * through the Auth Service internal API, which in turn verifies the
 * forwarded Bearer token.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserVerificationServiceImpl implements UserVerificationService {

    private final AuthServiceClient authServiceClient;

    @Override
    public void verifyActiveUser(Long authUserId, String systemRole) {

        AuthUserResponse user = fetchUser(authUserId);

        /*
         * Identity verification: the Auth Service returns the profile of
         * the token holder. The user ID must match X-User-Id, the role
         * must match X-User-Role and the email must be present. Any
         * mismatch means the gateway headers cannot be trusted.
         */
        if (user == null
                || !authUserId.equals(user.getUserId())
                || !roleMatches(systemRole, user.getRole())
                || user.getEmail() == null
                || user.getEmail().isBlank()) {

            throw new UnauthorizedUserException(
                    "Authenticated user could not be verified with the "
                            + "Auth Service");
        }

        if (!user.isActive()) {

            log.warn("Inactive user {} attempted a payment operation",
                    authUserId);

            throw new InactiveUserException(authUserId);
        }

        log.debug("User {} verified with Auth Service (role={})",
                authUserId, user.getRole());
    }

    /**
     * Calls the Auth Service internal endpoint and translates HTTP
     * failures into the matching VOLTARAS exceptions.
     */
    private AuthUserResponse fetchUser(Long authUserId) {

        try {

            return authServiceClient.getInternalUser(authUserId);

        } catch (FeignException.NotFound ex) {

            throw new UserNotFoundException(authUserId);

        } catch (FeignException.Unauthorized
                 | FeignException.Forbidden ex) {

            // The forwarded token was rejected or the requested user ID
            // does not match the token holder.
            throw new UnauthorizedUserException(
                    "Authenticated user could not be verified with the "
                            + "Auth Service");

        } catch (FeignException ex) {

            log.error("Auth Service failure while verifying user {}: {}",
                    authUserId, ex.getMessage());

            throw new UpstreamServiceException(
                    "Auth Service is unavailable", ex);
        }
    }

    /**
     * Compares the gateway-injected X-User-Role with the role stored in
     * the Auth Service. Both {@code ADMIN} and {@code ROLE_ADMIN} spell
     * the same role and are accepted.
     */
    private boolean roleMatches(String systemRole, String authRole) {

        if (systemRole == null || authRole == null) {
            return false;
        }

        return normalizeRole(systemRole)
                .equals(normalizeRole(authRole));
    }

    private String normalizeRole(String role) {

        String normalized = role.trim().toUpperCase();

        if (normalized.startsWith("ROLE_")) {
            normalized = normalized.substring("ROLE_".length());
        }

        return normalized;
    }
}
