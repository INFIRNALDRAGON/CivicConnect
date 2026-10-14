package com.civicconnect.backend.controller;

import com.civicconnect.backend.dto.CategoryDto;
import com.civicconnect.backend.service.CategoryService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService service;

    public CategoryController(CategoryService service) {
        this.service = service;
    }

    /** The controlled category list the requester picks from (FR-002). */
    @GetMapping
    public List<CategoryDto> list() {
        return service.listActive();
    }
}
