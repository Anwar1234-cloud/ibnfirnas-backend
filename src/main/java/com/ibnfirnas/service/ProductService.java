package com.ibnfirnas.service;

import com.ibnfirnas.dto.request.ProductRequest;
import com.ibnfirnas.dto.response.ProductResponse;
import com.ibnfirnas.entity.Category;
import com.ibnfirnas.entity.Product;
import com.ibnfirnas.entity.ProductImage;
import com.ibnfirnas.exception.ResourceNotFoundException;
import com.ibnfirnas.repository.CategoryRepository;
import com.ibnfirnas.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    // ============ toDTO ============
    public ProductResponse toDTO(Product product) {
        if (product == null) return null;

        String primaryImageUrl = null;
        if (product.getImages() != null && !product.getImages().isEmpty()) {
            primaryImageUrl = product.getImages().stream()
                    .filter(img -> img.getIsPrimary() != null && img.getIsPrimary())
                    .findFirst()
                    .map(ProductImage::getImageUrl)
                    .orElse(product.getImages().get(0).getImageUrl());
        }

        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .slug(product.getSlug())
                .description(product.getDescription())
                .shortDescription(product.getShortDescription())
                .price(product.getPrice())
                .discountPrice(product.getDiscountPrice())
                .sku(product.getSku())
                .stockQuantity(product.getStockQuantity())
                .stockStatus(product.getStockStatus())
                .isFeatured(product.getIsFeatured())
                .isActive(product.getIsActive())
                .categoryId(product.getCategory() != null
                        ? product.getCategory().getId() : null)
                .categoryName(product.getCategory() != null
                        ? product.getCategory().getName() : null)
                .averageRating(product.getAverageRating())
                .totalReviews(product.getTotalReviews())
                .primaryImageUrl(primaryImageUrl)
                .createdAt(product.getCreatedAt())
                .build();
    }

    // ============ toEntity ============
    public Product toEntity(ProductRequest request) {
        if (request == null) return null;

        Category category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId())
                    .orElse(null);
        }

        return Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .shortDescription(request.getShortDescription())
                .price(request.getPrice())
                .discountPrice(request.getDiscountPrice())
                .sku(request.getSku())
                .stockQuantity(request.getStockQuantity() != null
                        ? request.getStockQuantity() : 0)
                .isFeatured(request.getIsFeatured() != null
                        ? request.getIsFeatured() : false)
                .isActive(request.getIsActive() != null
                        ? request.getIsActive() : true)
                .category(category)
                .build();
    }

    // ============ CRUD ============
    public List<ProductResponse> getAllProducts() {
        return productRepository.findByIsActiveTrue()
                .stream().map(this::toDTO)
                .collect(Collectors.toList());
    }

    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found with id: " + id));
        return toDTO(product);
    }

    public List<ProductResponse> getFeaturedProducts() {
        return productRepository.findByIsFeaturedTrue()
                .stream().map(this::toDTO)
                .collect(Collectors.toList());
    }

    public ProductResponse createProduct(ProductRequest request) {
        Product product = toEntity(request);
        return toDTO(productRepository.save(product));
    }

    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found with id: " + id));

        if (request.getName() != null) product.setName(request.getName());
        if (request.getDescription() != null)
            product.setDescription(request.getDescription());
        if (request.getShortDescription() != null)
            product.setShortDescription(request.getShortDescription());
        if (request.getPrice() != null) product.setPrice(request.getPrice());
        if (request.getDiscountPrice() != null)
            product.setDiscountPrice(request.getDiscountPrice());
        if (request.getSku() != null) product.setSku(request.getSku());
        if (request.getStockQuantity() != null)
            product.setStockQuantity(request.getStockQuantity());
        if (request.getIsFeatured() != null)
            product.setIsFeatured(request.getIsFeatured());
        if (request.getIsActive() != null)
            product.setIsActive(request.getIsActive());
        if (request.getCategoryId() != null) {
            categoryRepository.findById(request.getCategoryId())
                    .ifPresent(product::setCategory);
        }

        return toDTO(productRepository.save(product));
    }

    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Product not found with id: " + id);
        }
        productRepository.deleteById(id);
    }
}