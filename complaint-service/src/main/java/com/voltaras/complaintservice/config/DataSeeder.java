package com.voltaras.complaintservice.config;

import com.voltaras.complaintservice.entity.ComplaintCategory;
import com.voltaras.complaintservice.enums.ComplaintCategoryName;
import com.voltaras.complaintservice.repository.ComplaintCategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Seeds the four standard complaint categories on startup when they do
 * not exist yet (same pattern as the Auth Service role seeding).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final ComplaintCategoryRepository categoryRepository;

    @Override
    public void run(String... args) {

        Arrays.stream(ComplaintCategoryName.values())
                .forEach(this::seedCategory);
    }

    private void seedCategory(ComplaintCategoryName name) {

        if (categoryRepository.existsByName(name.name())) {
            return;
        }

        ComplaintCategory category = ComplaintCategory.builder()
                .name(name.name())
                .description(name.getDescription())
                .active(true)
                .build();

        categoryRepository.save(category);

        log.info("Seeded complaint category: {}", name.name());
    }
}
