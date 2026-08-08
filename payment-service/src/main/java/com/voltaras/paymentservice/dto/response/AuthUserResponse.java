package com.voltaras.paymentservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Verified user profile returned by the Auth Service internal endpoint
 * ({@code GET /api/auth/internal/users/{userId}}). This is the single
 * source of truth for whether the authenticated user exists and is
 * active before a recharge or a wallet bill payment is allowed.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthUserResponse {

    private Long userId;
    private String email;
    private String fullName;
    private String role;
    private boolean active;
}
