package com.ibnfirnas.controller;

import com.ibnfirnas.dto.request.ProductRequest;
import com.ibnfirnas.dto.response.ApiResponse;
import com.ibnfirnas.entity.Product;
import com.ibnfirnas.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Product>>> getAllProducts() {
        return ResponseEntity.ok(
                ApiResponse.success("Products fetched", productService.getAllProducts()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Product>> getProduct(@PathVariable Long id) {
        return ResponseEntity.ok(
                ApiResponse.success("Product fetched", productService.getProductById(id)));
    }

    @GetMapping("/featured")
    public ResponseEntity<ApiResponse<List<Product>>> getFeatured() {
        return ResponseEntity.ok(
                ApiResponse.success("Featured products", productService.getFeaturedProducts()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Product>> createProduct(
            @RequestBody ProductRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("Product created",
                        productService.createProduct(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Product>> updateProduct(
            @PathVariable Long id,
            @RequestBody ProductRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("Product updated",
                        productService.updateProduct(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(
            @PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.success("Product deleted", null));
    }
}