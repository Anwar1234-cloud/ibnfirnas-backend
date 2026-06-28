package com.ibnfirnas.service;

import com.ibnfirnas.entity.Category;
import com.ibnfirnas.exception.ResourceNotFoundException;
import com.ibnfirnas.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public List<Category> getAllCategories() {
        return categoryRepository.findByIsActiveTrue();
    }

    public List<Category> getRootCategories() {
        return categoryRepository.findByParentIsNull();
    }

    public Category getCategoryById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
    }

    public Category saveCategory(Category category) {
        return categoryRepository.save(category);
    }

    public void deleteCategory(Long id) {
        categoryRepository.deleteById(id);
    }
}
