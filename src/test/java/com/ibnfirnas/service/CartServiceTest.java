package com.ibnfirnas.service;

import com.ibnfirnas.dto.request.CartRequest;
import com.ibnfirnas.dto.response.CartResponse;
import com.ibnfirnas.entity.*;
import com.ibnfirnas.exception.BadRequestException;
import com.ibnfirnas.exception.ResourceNotFoundException;
import com.ibnfirnas.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Cart Service Tests")
class CartServiceTest {

    @Mock private CartRepository cartRepository;
    @Mock private CartItemRepository cartItemRepository;
    @Mock private ProductRepository productRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private CartService cartService;

    private User mockUser;
    private Product mockProduct;
    private Cart mockCart;
    private CartRequest cartRequest;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .id(1L)
                .email("anwar@test.com")
                .build();

        mockProduct = Product.builder()
                .id(1L)
                .name("Automatic Gate")
                .price(new BigDecimal("2500.00"))
                .isActive(true)
                .build();

        mockCart = Cart.builder()
                .id(1L)
                .user(mockUser)
                .totalItems(0)
                .subtotal(BigDecimal.ZERO)
                .total(BigDecimal.ZERO)
                .items(new ArrayList<>())
                .build();

        cartRequest = new CartRequest(1L, 2);
    }

    @Test
    @DisplayName("Get or create cart — existing cart")
    void getOrCreateCart_ExistingCart() {
        when(userRepository.findByEmail("anwar@test.com"))
                .thenReturn(Optional.of(mockUser));
        when(cartRepository.findByUserId(1L))
                .thenReturn(Optional.of(mockCart));


        CartResponse response = cartService.getOrCreateCart("anwar@test.com");

        assertNotNull(response);
        assertEquals(1L, response.getId());
        verify(cartRepository, never()).save(any());
    }

    @Test
    @DisplayName("Get or create cart — new cart")
    void getOrCreateCart_NewCart() {
        when(userRepository.findByEmail("anwar@test.com"))
                .thenReturn(Optional.of(mockUser));
        when(cartRepository.findByUserId(1L))
                .thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenReturn(mockCart);


        CartResponse response = cartService.getOrCreateCart("anwar@test.com");

        assertNotNull(response);
        verify(cartRepository, times(1)).save(any(Cart.class));
    }

    @Test
    @DisplayName("Add to cart — success")
    void addToCart_Success() {
        CartItem cartItem = CartItem.builder()
                .id(1L)
                .cart(mockCart)
                .product(mockProduct)
                .quantity(2)
                .unitPrice(new BigDecimal("2500.00"))
                .totalPrice(new BigDecimal("5000.00"))
                .build();

        when(userRepository.findByEmail("anwar@test.com"))
                .thenReturn(Optional.of(mockUser));
        when(productRepository.findById(1L))
                .thenReturn(Optional.of(mockProduct));
        when(cartRepository.findByUserId(1L))
                .thenReturn(Optional.of(mockCart));
        when(cartItemRepository.save(any(CartItem.class))).thenReturn(cartItem);
        when(cartItemRepository.findByCartId(1L)).thenReturn(List.of(cartItem));
        when(cartRepository.save(any(Cart.class))).thenReturn(mockCart);

        CartResponse response = cartService.addToCart(cartRequest, "anwar@test.com");

        assertNotNull(response);
        verify(cartItemRepository, times(1)).save(any(CartItem.class));
    }

    @Test
    @DisplayName("Add to cart — product not found")
    void addToCart_ProductNotFound() {
        when(userRepository.findByEmail("anwar@test.com"))
                .thenReturn(Optional.of(mockUser));
        when(productRepository.findById(999L))
                .thenReturn(Optional.empty());

        CartRequest badRequest = new CartRequest(999L, 1);

        assertThrows(ResourceNotFoundException.class,
                () -> cartService.addToCart(badRequest, "anwar@test.com"));
    }

    @Test
    @DisplayName("Add to cart — invalid quantity")
    void addToCart_InvalidQuantity() {
        when(userRepository.findByEmail("anwar@test.com"))
                .thenReturn(Optional.of(mockUser));

        CartRequest badRequest = new CartRequest(1L, 0);

        assertThrows(BadRequestException.class,
                () -> cartService.addToCart(badRequest, "anwar@test.com"));
    }

    @Test
    @DisplayName("Clear cart — success")
    void clearCart_Success() {
        when(userRepository.findByEmail("anwar@test.com"))
                .thenReturn(Optional.of(mockUser));
        when(cartRepository.findByUserId(1L))
                .thenReturn(Optional.of(mockCart));


        assertDoesNotThrow(() -> cartService.clearCart("anwar@test.com"));
        verify(cartRepository, times(1)).save(any(Cart.class));
    }
}