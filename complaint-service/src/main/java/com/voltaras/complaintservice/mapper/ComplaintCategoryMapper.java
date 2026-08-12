package com.voltaras.complaintservice.mapper;

import com.voltaras.complaintservice.dto.response.CategoryResponse;
import com.voltaras.complaintservice.entity.ComplaintCategory;

/**
 * Manual conversion between {@link ComplaintCategory} and its response DTO.
 */
public final class ComplaintCategoryMapper {

    private ComplaintCategoryMapper() {
        // Prevent object creation for utility class.
    }

    public static CategoryResponse toResponse(ComplaintCategory category) {

        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .build();
    }
}
