package com.civicconnect.backend.repository;

import com.civicconnect.backend.model.Category;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, UUID> {
    List<Category> findByActiveTrueOrderByNameAsc();
    Optional<Category> findByIdAndActiveTrue(UUID id);
}
