package com.example.expenses.service;

import static com.example.expenses.service.Errors.*;

import com.example.expenses.dto.Dtos.*;
import com.example.expenses.model.Category;
import com.example.expenses.repo.CategoryRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CategoryService {
    private final CategoryRepository categories;
    public CategoryService(CategoryRepository categories) { this.categories = categories; }

    public CategoryResponse create(String rawName) {
        String name = rawName == null ? "" : rawName.trim();
        if (name.isEmpty()) throw bad("Name is required");
        if (name.length() > 60) throw bad("Name too long (max 60)");
        if (categories.existsByNameIgnoreCase(name)) throw conflict("Category already exists");
        Category c = categories.save(new Category(name));
        return new CategoryResponse(c.getId(), c.getName());
    }

    public List<CategoryResponse> list() {
        return categories.findAllByOrderByNameAsc().stream().map(c -> new CategoryResponse(c.getId(), c.getName())).toList();
    }
}
