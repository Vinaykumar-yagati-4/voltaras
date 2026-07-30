package com.voltaras.authservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoResponse {

    private Long userId;
    private String email;
    private String role;
    private boolean isActive;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
}
