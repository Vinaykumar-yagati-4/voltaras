package com.voltaras.userservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * API response representing a user profile.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserProfileResponse {

    private Long id;

    private Long authUserId;

    private String fullName;

    private String phone;

    private String address;

    private String city;

    private String state;

    private String country;

    private String postalCode;

    private String profileImage;

    private LocalDate dateOfBirth;

    private String gender;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
