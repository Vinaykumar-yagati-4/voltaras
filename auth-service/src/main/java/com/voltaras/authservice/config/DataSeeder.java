package com.voltaras.authservice.config;

import com.voltaras.authservice.entity.Role;
import com.voltaras.authservice.entity.User;
import com.voltaras.authservice.entity.UserRole;
import com.voltaras.authservice.enums.RoleType;
import com.voltaras.authservice.repository.RoleRepository;
import com.voltaras.authservice.repository.UserRepository;
import com.voltaras.authservice.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${FIRST_ADMIN_EMAIL:}")
    private String firstAdminEmail;

    @Value("${FIRST_ADMIN_PASSWORD:}")
    private String firstAdminPassword;

    @Value("${FIRST_ADMIN_FULL_NAME:Voltaras Platform Admin}")
    private String firstAdminFullName;

    @Override
    @Transactional
    public void run(String... args) {

        log.info("Checking and seeding default roles...");

        Role consumerRole = seedRole(
                RoleType.CONSUMER,
                "Regular platform user who can submit readings, view bills, make payments, and raise complaints"
        );

        Role adminRole = seedRole(
                RoleType.ADMIN,
                "Platform administrator who manages users, tariffs, bills, payments, complaints, and notifications"
        );

        log.info("Role seeding completed.");

        seedFirstAdmin(adminRole, consumerRole);
    }

    private Role seedRole(RoleType name, String description) {
        return roleRepository.findByName(name)
                .orElseGet(() -> {
                    Role role = Role.builder()
                            .name(name)
                            .description(description)
                            .build();

                    Role savedRole = roleRepository.save(role);
                    log.info("Seeded role: {}", name);
                    return savedRole;
                });
    }

    private void seedFirstAdmin(Role adminRole, Role consumerRole) {

        if (userRoleRepository.existsByRole_Name(RoleType.ADMIN)) {
            log.info("Platform ADMIN already exists. Skipping first admin bootstrap.");
            return;
        }

        if (isBlank(firstAdminEmail) || isBlank(firstAdminPassword)) {
            log.warn(
                    "No platform ADMIN exists, but FIRST_ADMIN_EMAIL or FIRST_ADMIN_PASSWORD is missing. " +
                            "Set these environment variables before first production deployment."
            );
            return;
        }

        String normalizedEmail = firstAdminEmail.trim().toLowerCase();

        User adminUser = userRepository.findByEmail(normalizedEmail)
                .orElseGet(() -> {
                    User newAdmin = User.builder()
                            .fullName(firstAdminFullName)
                            .email(normalizedEmail)
                            .passwordHash(passwordEncoder.encode(firstAdminPassword))
                            .isActive(true)
                            .build();

                    User savedUser = userRepository.save(newAdmin);
                    log.info("Created first platform ADMIN user from environment: {}", normalizedEmail);
                    return savedUser;
                });

        UserRole adminUserRole = UserRole.builder()
                .id(new UserRole.UserRoleId(adminUser.getId(), adminRole.getId()))
                .user(adminUser)
                .role(adminRole)
                .build();

        userRoleRepository.save(adminUserRole);

        log.info("Assigned ADMIN role to first platform admin user: {}", normalizedEmail);

        if (consumerRole != null) {
            log.debug("Consumer role remains available for public registration: {}", consumerRole.getName());
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
