package com.civicconnect.backend.service;

import com.civicconnect.backend.dto.CategoryDto;
import com.civicconnect.backend.repository.CategoryRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryService {

    private final CategoryRepository categories;

    public CategoryService(CategoryRepository categories) {
        this.categories = categories;
    }

    @Transactional(readOnly = true)
    public List<CategoryDto> listActive() {
        return categories.findByActiveTrueOrderByNameAsc().stream().map(CategoryDto::from).toList();
    }
}
