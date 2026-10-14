package com.civicconnect.backend.dto;

import com.civicconnect.backend.model.Category;
import java.util.UUID;

public record CategoryDto(UUID id, String name) {
    public static CategoryDto from(Category c) {
        return new CategoryDto(c.getId(), c.getName());
    }
}
