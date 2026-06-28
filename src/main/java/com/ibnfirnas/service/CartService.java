package com.ibnfirnas.service;

import com.ibnfirnas.dto.request.CartRequest;
import com.ibnfirnas.entity.*;
import com.ibnfirnas.exception.ResourceNotFoundException;
import com.ibnfirnas.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public Cart getOrCreateCart(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return cartRepository.findByUserId(user.getId())
                .orElseGet(() -> cartRepository.save(
                        Cart.builder().user(user).build()));
    }

    public Cart addToCart(CartRequest request, String userEmail) {
        Cart cart = getOrCreateCart(userEmail);
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        BigDecimal unitPrice = product.getDiscountPrice() != null
                ? product.getDiscountPrice() : product.getPrice();

        CartItem item = CartItem.builder()
                .cart(cart)
                .product(product)
                .quantity(request.getQuantity())
                .unitPrice(unitPrice)
                .totalPrice(unitPrice.multiply(BigDecimal.valueOf(request.getQuantity())))
                .build();

        cartItemRepository.save(item);
        return recalculate(cart);
    }

    public Cart removeFromCart(Long cartItemId, String userEmail) {
        cartItemRepository.deleteById(cartItemId);
        Cart cart = getOrCreateCart(userEmail);
        return recalculate(cart);
    }

    public void clearCart(String userEmail) {
        Cart cart = getOrCreateCart(userEmail);
        cart.getItems().clear();
        cartRepository.save(cart);
    }

    private Cart recalculate(Cart cart) {
        BigDecimal subtotal = cartItemRepository.findByCartId(cart.getId())
                .stream()
                .map(CartItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        cart.setSubtotal(subtotal);
        cart.setTotal(subtotal);
        cart.setTotalItems(cartItemRepository.findByCartId(cart.getId()).size());
        return cartRepository.save(cart);
    }
}
