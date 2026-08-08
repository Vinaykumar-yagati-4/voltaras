package com.voltaras.paymentservice.client;

import com.voltaras.paymentservice.dto.response.AuthUserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Feign client for the VOLTARAS Auth Service.
 *
 * <p>
 * The client is resolved through Eureka using the service name
 * {@code auth-service} (load-balanced by Spring Cloud LoadBalancer). The
 * Bearer access token of the incoming request is forwarded automatically
 * by the request interceptor in {@code FeignConfig}, so the Auth Service
 * internal endpoint always receives a valid JWT.
 * </p>
 *
 * <p>
 * This is the only allowed way for the Payment Service to look up user
 * data: the payment database never contains auth users, and the Auth
 * Service {@code auth_db.users} table is never read directly.
 * </p>
 */
@FeignClient(name = "auth-service")
public interface AuthServiceClient {

    /**
     * Returns the internal profile (userId, email, fullName, role,
     * active) of the user with the given ID.
     *
     * <p>
     * The Auth Service enforces that the requested ID matches the
     * forwarded access token, so this can only ever return the caller's
     * own profile.
     * </p>
     *
     * @param userId user ID from the X-User-Id gateway header
     * @return the verified user profile
     */
    @GetMapping("/api/auth/internal/users/{userId}")
    AuthUserResponse getInternalUser(
            @PathVariable("userId") Long userId);
}
