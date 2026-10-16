package com.civicconnect.backend.support;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.civicconnect.backend.request.Category;
import com.civicconnect.backend.request.CategoryCatalog;

public class InMemoryCategoryCatalog implements CategoryCatalog {

    private final Map<UUID, Category> categories = new ConcurrentHashMap<>();

    public void add(Category category) {
        categories.put(category.id(), category);
    }

    @Override
    public Optional<Category> findById(UUID id) {
        return Optional.ofNullable(categories.get(id));
    }

    @Override
    public List<Category> all() {
        return List.copyOf(categories.values());
    }
}
