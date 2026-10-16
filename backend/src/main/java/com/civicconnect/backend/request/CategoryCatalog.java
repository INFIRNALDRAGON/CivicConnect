package com.civicconnect.backend.request;

import java.util.List;
<<<<<<< Updated upstream
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

/**
 * The controlled category list (FR-002, DEC-01). In-memory stand-in: the real list
 * will be read from the categories table.
 */
@Component
public class CategoryCatalog {

    private final Map<Long, Category> categories = new ConcurrentHashMap<>();

    public void add(Category category) {
        categories.put(category.id(), category);
    }

    public Optional<Category> findById(Long id) {
        return Optional.ofNullable(categories.get(id));
    }

    public Optional<Category> findActive(Long id) {
        return findById(id).filter(Category::active);
    }

    public List<Category> allActive() {
        return categories.values().stream().filter(Category::active)
=======
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
>>>>>>> Stashed changes
                .sorted(java.util.Comparator.comparing(Category::name)).toList();
    }
}
