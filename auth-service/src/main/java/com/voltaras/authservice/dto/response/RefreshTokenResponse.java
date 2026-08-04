package com.voltaras.authservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenResponse {

    private String accessToken;

    private String refreshToken;

    private String tokenType;

    private long accessTokenExpiresIn;

    private long refreshTokenExpiresIn;

    private Long userId;

    private String role;

    private String email;

    private String message;
}
