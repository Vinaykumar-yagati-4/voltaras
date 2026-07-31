package com.voltaras.userservice.repository;

import com.voltaras.userservice.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

    boolean existsByAuthUserId(Long authUserId);

    Optional<UserProfile> findByAuthUserId(Long authUserId);
}
