package com.voltaras.authservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String token;

    private String tokenType;

    private long expiresIn;

    private Long userId;

    private String role;

    private String email;

    private String fullName;

    private String message;
}