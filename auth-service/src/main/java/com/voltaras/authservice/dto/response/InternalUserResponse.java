package com.voltaras.authservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Internal user profile returned by
 * {@code GET /api/auth/internal/users/{userId}}.
 *
 * <p>
 * Used for service-to-service communication only: other VOLTARAS services
 * (e.g. the Payment Service) call this endpoint to verify that a user
 * exists and is active before performing wallet operations.
 * </p>
 *
 * <p>
 * The JSON field is deliberately named {@code active} (not
 * {@code isActive}) so it matches the contract expected by consuming
 * services (see the Payment Service {@code AuthUserResponse} DTO).
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InternalUserResponse {

    private Long userId;

    private String email;

    private String fullName;

    /**
     * First assigned role (CONSUMER when the user has no role mappings).
     */
    private String role;

    /**
     * Always present: defaults to {@code true} when the stored value is
     * missing.
     */
    private boolean active;
}
