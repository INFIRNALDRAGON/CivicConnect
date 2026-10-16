package com.civicconnect.backend.request;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class JpaCategoryCatalog implements CategoryCatalog {

    private final CategoryRepository categories;

    JpaCategoryCatalog(CategoryRepository categories) {
        this.categories = categories;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Category> findById(UUID id) {
        return id == null ? Optional.empty() : categories.findById(id).map(JpaCategoryCatalog::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Category> all() {
        return categories.findAll().stream().map(JpaCategoryCatalog::toDomain).toList();
    }

    private static Category toDomain(CategoryEntity e) {
        return new Category(e.id, e.name, e.active, e.overdueAfterHours);
    }
}
