package com.voltaras.authservice.config;

import com.voltaras.authservice.entity.Role;
import com.voltaras.authservice.enums.RoleType;
import com.voltaras.authservice.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final RoleRepository roleRepository;

    @Override
    public void run(String... args) {

        log.info("Checking and seeding default roles...");

        // Seed CONSUMER role
        seedRole(RoleType.CONSUMER, "Regular platform user who can submit readings, view bills, make payments, and raise complaints");

        // Seed ADMIN role
        seedRole(RoleType.ADMIN, "Platform administrator who manages users, tariffs, bills, payments, complaints, and notifications");

        log.info("Role seeding completed.");
    }

    /**
     * Seeds a role if it doesn't already exist in the database.
     *
     * @param name        the role name (enum value)
     * @param description the human-readable description
     */
    private void seedRole(RoleType name, String description) {
        if (roleRepository.findByName(name).isEmpty()) {
            Role role = Role.builder()
                    .name(name)
                    .description(description)
                    .build();

            roleRepository.save(role);
            log.info("Seeded role: {}", name);
        } else {
            log.debug("Role already exists: {}", name);
        }
    }
}
