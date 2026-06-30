package com.ibnfirnas.service;

import com.ibnfirnas.dto.request.ProductRequest;
import com.ibnfirnas.entity.Category;
import com.ibnfirnas.entity.Product;
import com.ibnfirnas.exception.ResourceNotFoundException;
import com.ibnfirnas.repository.CategoryRepository;
import com.ibnfirnas.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public List<Product> getAllProducts() {
        return productRepository.findByIsActiveTrue();
    }

    public Product getProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
    }

    public List<Product> getFeaturedProducts() {
        return productRepository.findByIsFeaturedTrue();
    }

    public Product createProduct(ProductRequest request) {
        Category category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId())
                    .orElse(null);
        }
        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .shortDescription(request.getShortDescription())
                .price(request.getPrice())
                .discountPrice(request.getDiscountPrice())
                .sku(request.getSku())
                .stockQuantity(request.getStockQuantity())
                .isFeatured(request.getIsFeatured() != null ? request.getIsFeatured() : false)
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .category(category)
                .build();
        return productRepository.save(product);
    }

    public Product updateProduct(Long id, ProductRequest request) {
        Product product = getProductById(id);
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setShortDescription(request.getShortDescription());
        product.setPrice(request.getPrice());
        product.setDiscountPrice(request.getDiscountPrice());
        product.setSku(request.getSku());
        product.setStockQuantity(request.getStockQuantity());
        if (request.getIsFeatured() != null) product.setIsFeatured(request.getIsFeatured());
        if (request.getIsActive() != null) product.setIsActive(request.getIsActive());
        return productRepository.save(product);
    }

    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
    }
}
