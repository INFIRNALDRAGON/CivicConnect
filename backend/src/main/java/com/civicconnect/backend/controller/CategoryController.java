package com.civicconnect.backend.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.civicconnect.backend.request.RequestService;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final RequestService requests;

    public CategoryController(RequestService requests) {
        this.requests = requests;
    }

    @GetMapping
    public List<Map<String, Object>> list() {
        return requests.activeCategories().stream().map(c -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", c.id().toString());
            item.put("name", c.name());
            return item;
        }).toList();
    }
}
