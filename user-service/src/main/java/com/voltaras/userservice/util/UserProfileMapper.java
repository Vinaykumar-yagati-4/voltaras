package com.voltaras.userservice.util;

import com.voltaras.userservice.dto.request.CreateUserProfileRequest;
import com.voltaras.userservice.dto.request.UpdateUserProfileRequest;
import com.voltaras.userservice.dto.response.UserProfileResponse;
import com.voltaras.userservice.entity.UserProfile;

public final class UserProfileMapper {

    private UserProfileMapper() {
        // Prevent object creation for utility class
    }

    public static UserProfile toEntity(
            CreateUserProfileRequest request
    ) {

        return UserProfile.builder()
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .address(request.getAddress())
                .city(request.getCity())
                .state(request.getState())
                .country(request.getCountry())
                .postalCode(request.getPostalCode())
                .profileImage(request.getProfileImage())
                .dateOfBirth(request.getDateOfBirth())
                .gender(request.getGender())
                .build();
    }

    /**
     * Copies update DTO fields into an existing entity.
     */
    public static void copyToEntity(
            UpdateUserProfileRequest request,
            UserProfile profile
    ) {

        profile.setFullName(request.getFullName());
        profile.setPhone(request.getPhone());
        profile.setAddress(request.getAddress());
        profile.setCity(request.getCity());
        profile.setState(request.getState());
        profile.setCountry(request.getCountry());
        profile.setPostalCode(request.getPostalCode());
        profile.setProfileImage(request.getProfileImage());
        profile.setDateOfBirth(request.getDateOfBirth());
        profile.setGender(request.getGender());
    }

    /**
     * Converts entity into response DTO.
     */
    public static UserProfileResponse toResponse(
            UserProfile profile
    ) {

        return UserProfileResponse.builder()
                .id(profile.getId())
                .authUserId(profile.getAuthUserId())
                .fullName(profile.getFullName())
                .phone(profile.getPhone())
                .address(profile.getAddress())
                .city(profile.getCity())
                .state(profile.getState())
                .country(profile.getCountry())
                .postalCode(profile.getPostalCode())
                .profileImage(profile.getProfileImage())
                .dateOfBirth(profile.getDateOfBirth())
                .gender(profile.getGender())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }
}