package com.civicconnect.backend.request;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryCatalog {

    Optional<Category> findById(UUID id);

    List<Category> all();

    default Optional<Category> findActive(UUID id) {
        return findById(id).filter(Category::active);
    }

    default List<Category> allActive() {
        return all().stream().filter(Category::active)
                .sorted(java.util.Comparator.comparing(Category::name)).toList();
    }
}
