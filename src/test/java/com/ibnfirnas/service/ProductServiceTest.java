package com.ibnfirnas.service;

import com.ibnfirnas.dto.request.ProductRequest;
import com.ibnfirnas.dto.response.PageResponse;
import com.ibnfirnas.dto.response.ProductResponse;
import com.ibnfirnas.entity.Product;
import com.ibnfirnas.exception.ResourceNotFoundException;
import com.ibnfirnas.repository.CategoryRepository;
import com.ibnfirnas.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Product Service Tests")
class ProductServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private CategoryRepository categoryRepository;

    @InjectMocks private ProductService productService;

    private Product mockProduct;
    private ProductRequest productRequest;

    @BeforeEach
    void setUp() {
        mockProduct = Product.builder()
                .id(1L)
                .name("Automatic Sliding Gate")
                .price(new BigDecimal("2500.00"))
                .sku("ASG-001")
                .stockQuantity(10)
                .isFeatured(true)
                .isActive(true)
                .build();

        productRequest = new ProductRequest();
        productRequest.setName("Automatic Sliding Gate");
        productRequest.setPrice(new BigDecimal("2500.00"));
        productRequest.setSku("ASG-001");
        productRequest.setStockQuantity(10);
        productRequest.setIsFeatured(true);
        productRequest.setIsActive(true);
    }

    @Test
    @DisplayName("Get all active products — success")
    void getAllProducts_Success() {
        when(productRepository.findByIsActiveTrue(any(Pageable.class)))
                .thenReturn(new PageImpl<>(Arrays.asList(mockProduct)));

        PageResponse<ProductResponse> products = productService.getAllProducts(0, 20, null);

        assertNotNull(products);
        assertEquals(1, products.getContent().size());
        assertEquals("Automatic Sliding Gate", products.getContent().get(0).getName());
        verify(productRepository, times(1)).findByIsActiveTrue(any(Pageable.class));
    }

    @Test
    @DisplayName("Get all products — empty list")
    void getAllProducts_EmptyList() {
        when(productRepository.findByIsActiveTrue(any(Pageable.class)))
                .thenReturn(Page.empty());

        PageResponse<ProductResponse> products = productService.getAllProducts(0, 20, null);

        assertNotNull(products);
        assertTrue(products.getContent().isEmpty());
    }

    @Test
    @DisplayName("Get product by ID — success")
    void getProductById_Success() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(mockProduct));

        ProductResponse response = productService.getProductById(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Automatic Sliding Gate", response.getName());
        assertEquals(new BigDecimal("2500.00"), response.getPrice());
    }

    @Test
    @DisplayName("Get product by ID — not found")
    void getProductById_NotFound_ThrowsException() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> productService.getProductById(999L));

        assertTrue(exception.getMessage().contains("999"));
    }

    @Test
    @DisplayName("Get featured products")
    void getFeaturedProducts_Success() {
        when(productRepository.findByIsFeaturedTrue())
                .thenReturn(Arrays.asList(mockProduct));

        List<ProductResponse> products = productService.getFeaturedProducts();

        assertNotNull(products);
        assertEquals(1, products.size());
        assertTrue(products.get(0).getIsFeatured());
    }

    @Test
    @DisplayName("Create product — success")
    void createProduct_Success() {
        when(productRepository.save(any(Product.class))).thenReturn(mockProduct);

        ProductResponse response = productService.createProduct(productRequest);

        assertNotNull(response);
        assertEquals("Automatic Sliding Gate", response.getName());
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    @DisplayName("Delete product — success")
    void deleteProduct_Success() {

        Product product = Product.builder()
                .id(1L)
                .name("Test Product")
                .isActive(true)
                .build();

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        when(productRepository.save(any(Product.class)))
                .thenReturn(product);

        assertDoesNotThrow(() -> productService.deleteProduct(1L));

        assertFalse(product.getIsActive());

        verify(productRepository, times(1)).findById(1L);
        verify(productRepository, times(1)).save(product);
    }

    @Test
    @DisplayName("Delete product — not found")
    void deleteProduct_NotFound_ThrowsException() {
        when(productRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> productService.deleteProduct(1L));

        verify(productRepository, never()).deleteById(anyLong());
    }
}