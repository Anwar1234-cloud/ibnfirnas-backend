package com.ibnfirnas.controller;

import com.ibnfirnas.dto.response.ApiResponse;
import com.ibnfirnas.entity.Category;
import com.ibnfirnas.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Category>>> getAll() {
        return ResponseEntity.ok(
                ApiResponse.success("Categories fetched",
                        categoryService.getAllCategories()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Category>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(
                ApiResponse.success("Category fetched",
                        categoryService.getCategoryById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Category>> create(
            @RequestBody Category category) {
        return ResponseEntity.ok(
                ApiResponse.success("Category created",
                        categoryService.saveCategory(category)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.ok(ApiResponse.success("Category deleted", null));
    }
}