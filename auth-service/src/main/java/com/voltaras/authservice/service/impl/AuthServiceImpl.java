package com.voltaras.authservice.service.impl;

import com.voltaras.authservice.dto.request.ChangePasswordRequest;
import com.voltaras.authservice.dto.request.LoginRequest;
import com.voltaras.authservice.dto.request.RegisterRequest;
import com.voltaras.authservice.dto.response.AuthResponse;
import com.voltaras.authservice.dto.response.UserInfoResponse;
import com.voltaras.authservice.entity.Role;
import com.voltaras.authservice.entity.User;
import com.voltaras.authservice.entity.UserRole;
import com.voltaras.authservice.enums.RoleType;
import com.voltaras.authservice.exception.BadRequestException;
import com.voltaras.authservice.exception.DuplicateResourceException;
import com.voltaras.authservice.exception.ResourceNotFoundException;
import com.voltaras.authservice.repository.RoleRepository;
import com.voltaras.authservice.repository.UserRepository;
import com.voltaras.authservice.security.JwtTokenProvider;
import com.voltaras.authservice.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final Logger log =
            LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {

        // Step 1: Validate password and confirm password
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Passwords do not match");
        }

        // Step 2: Validate full name
        if (request.getFullName() == null ||
                request.getFullName().trim().isEmpty()) {

            throw new BadRequestException("Full name is required");
        }

        // Step 3: Normalize the email
        String normalizedEmail =
                request.getEmail().toLowerCase().trim();

        // Step 4: Check email uniqueness
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new DuplicateResourceException(
                    "User",
                    "email",
                    normalizedEmail
            );
        }

        // Step 5: Hash password using BCrypt
        String hashedPassword =
                passwordEncoder.encode(request.getPassword());

        // Step 6: Find default CONSUMER role
        Role consumerRole = roleRepository
                .findByName(RoleType.CONSUMER)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Default role CONSUMER not found. " +
                                        "Seed the roles table first."
                        )
                );

        // Step 7: Create the User entity
        User user = User.builder()
                .fullName(request.getFullName().trim())
                .email(normalizedEmail)
                .passwordHash(hashedPassword)
                .isActive(true)
                .build();

        // Step 8: Create UserRole junction entity
        UserRole userRole = UserRole.builder()
                .user(user)
                .role(consumerRole)
                .build();

        user.setUserRoles(Set.of(userRole));

        // Step 9: Save User
        // Cascade will also save UserRole
        User savedUser = userRepository.save(user);

        log.info(
                "New user registered: email={}, role={}",
                savedUser.getEmail(),
                RoleType.CONSUMER
        );

        // Step 10: Return registration response
        // Token is not generated during registration
        return AuthResponse.builder()
                .userId(savedUser.getId())
                .fullName(savedUser.getFullName())
                .email(savedUser.getEmail())
                .role(RoleType.CONSUMER.name())
                .message("Registration successful. Please log in.")
                .build();
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {

        // Step 1: Normalize email
        String normalizedEmail =
                request.getEmail().toLowerCase().trim();

        // Step 2: Authenticate using Spring Security
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            normalizedEmail,
                            request.getPassword()
                    )
            );

        } catch (BadCredentialsException exception) {

            // Do not reveal whether email or password is incorrect
            throw new BadCredentialsException(
                    "Invalid email or password"
            );

        } catch (DisabledException exception) {

            throw new DisabledException(
                    "Account is deactivated"
            );
        }

        // Step 3: Load authenticated user
        User user = userRepository
                .findByEmail(normalizedEmail)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User",
                                "email",
                                normalizedEmail
                        )
                );

        // Step 4: Update last login date and time
        user.setLastLoginAt(LocalDateTime.now());

        userRepository.save(user);

        // Step 5: Generate JWT token
        String token =
                jwtTokenProvider.generateToken(user);

        // Step 6: Extract user role
        String role = user.getUserRoles()
                .stream()
                .findFirst()
                .map(userRole ->
                        userRole
                                .getRole()
                                .getName()
                                .name()
                )
                .orElse(RoleType.CONSUMER.name());

        log.info(
                "User logged in: email={}, role={}",
                user.getEmail(),
                role
        );

        // Step 7: Return login response with JWT
        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(
                        jwtTokenProvider.getExpirationMs() / 1000
                )
                .userId(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(role)
                .message("Login successful")
                .build();
    }

    @Override
    @Transactional
    public void changePassword(
            Long userId,
            ChangePasswordRequest request
    ) {

        // Step 1: Check new password and confirmation
        if (!request.getNewPassword()
                .equals(request.getConfirmNewPassword())) {

            throw new BadRequestException(
                    "New passwords do not match"
            );
        }

        // Step 2: Load user
        User user = userRepository
                .findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User",
                                "id",
                                userId
                        )
                );

        // Step 3: Validate existing password
        if (!passwordEncoder.matches(
                request.getCurrentPassword(),
                user.getPasswordHash()
        )) {

            throw new BadRequestException(
                    "Current password is incorrect"
            );
        }

        // Step 4: Hash and update new password
        user.setPasswordHash(
                passwordEncoder.encode(
                        request.getNewPassword()
                )
        );

        userRepository.save(user);

        log.info(
                "Password changed for userId={}",
                userId
        );
    }

    @Override
    @Transactional(readOnly = true)
    public UserInfoResponse getCurrentUser(String email) {

        String normalizedEmail =
                email.toLowerCase().trim();

        User user = userRepository
                .findByEmail(normalizedEmail)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User",
                                "email",
                                normalizedEmail
                        )
                );

        String role = user.getUserRoles()
                .stream()
                .findFirst()
                .map(userRole ->
                        userRole
                                .getRole()
                                .getName()
                                .name()
                )
                .orElse(RoleType.CONSUMER.name());

        return UserInfoResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .role(role)
                .isActive(user.getIsActive())
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .build();
    }
}