package com.ibnfirnas.service;

import com.ibnfirnas.dto.request.CartRequest;
import com.ibnfirnas.dto.response.CartResponse;
import com.ibnfirnas.entity.*;
import com.ibnfirnas.exception.BadRequestException;
import com.ibnfirnas.exception.ResourceNotFoundException;
import com.ibnfirnas.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    // ============ toDTO ============
    public CartResponse toDTO(Cart cart) {
        if (cart == null) return null;

        List<CartResponse.CartItemResponse> itemDTOs = null;
        if (cart.getItems() != null) {
            itemDTOs = cart.getItems().stream()
                    .map(item -> {
                        String imageUrl = null;
                        if (item.getProduct() != null
                                && item.getProduct().getImages() != null
                                && !item.getProduct().getImages().isEmpty()) {
                            imageUrl = item.getProduct().getImages()
                                    .get(0).getImageUrl();
                        }
                        return CartResponse.CartItemResponse.builder()
                                .id(item.getId())
                                .productId(item.getProduct() != null
                                        ? item.getProduct().getId() : null)
                                .productName(item.getProduct() != null
                                        ? item.getProduct().getName() : null)
                                .productImage(imageUrl)
                                .quantity(item.getQuantity())
                                .unitPrice(item.getUnitPrice())
                                .totalPrice(item.getTotalPrice())
                                .build();
                    })
                    .collect(Collectors.toList());
        }

        return CartResponse.builder()
                .id(cart.getId())
                .totalItems(cart.getTotalItems())
                .subtotal(cart.getSubtotal())
                .total(cart.getTotal())
                .items(itemDTOs)
                .updatedAt(cart.getUpdatedAt())
                .build();
    }

    // ============ CRUD ============
    public CartResponse getOrCreateCart(String userEmail) {
        User user = getUser(userEmail);
        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseGet(() -> cartRepository.save(
                        Cart.builder().user(user).build()));
        return toDTO(cart);
    }

    public CartResponse addToCart(CartRequest request, String userEmail) {
        User user = getUser(userEmail);

        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new BadRequestException("Quantity must be greater than 0");
        }

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found with id: " + request.getProductId()));

        if (!product.getIsActive()) {
            throw new BadRequestException("Product is not available");
        }

        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseGet(() -> cartRepository.save(
                        Cart.builder().user(user).build()));

        BigDecimal unitPrice = product.getDiscountPrice() != null
                ? product.getDiscountPrice() : product.getPrice();

        CartItem item = CartItem.builder()
                .cart(cart)
                .product(product)
                .quantity(request.getQuantity())
                .unitPrice(unitPrice)
                .totalPrice(unitPrice.multiply(
                        BigDecimal.valueOf(request.getQuantity())))
                .build();

        cartItemRepository.save(item);
        return toDTO(recalculate(cart));
    }

    public CartResponse removeFromCart(Long cartItemId, String userEmail) {
        User user = getUser(userEmail);
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cart item not found with id: " + cartItemId));

        Cart cart = item.getCart();
        if (!cart.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("Not authorized to remove this item");
        }

        cartItemRepository.deleteById(cartItemId);
        return toDTO(recalculate(cart));
    }

    public void clearCart(String userEmail) {
        User user = getUser(userEmail);
        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cart not found for user: " + userEmail));

        List<CartItem> items = cartItemRepository.findByCartId(cart.getId());
        cartItemRepository.deleteAll(items);

        cart.setTotalItems(0);
        cart.setSubtotal(BigDecimal.ZERO);
        cart.setTotal(BigDecimal.ZERO);
        cartRepository.save(cart);
    }

    private Cart recalculate(Cart cart) {
        List<CartItem> items = cartItemRepository.findByCartId(cart.getId());
        BigDecimal subtotal = items.stream()
                .map(CartItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        cart.setSubtotal(subtotal);
        cart.setTotal(subtotal);
        cart.setTotalItems(items.size());
        return cartRepository.save(cart);
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with email: " + email));
    }
}