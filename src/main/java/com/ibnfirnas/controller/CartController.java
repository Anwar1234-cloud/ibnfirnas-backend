package com.ibnfirnas.controller;

import com.ibnfirnas.dto.request.CartRequest;
import com.ibnfirnas.dto.response.ApiResponse;
import com.ibnfirnas.entity.Cart;
import com.ibnfirnas.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<ApiResponse<Cart>> getCart(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                ApiResponse.success("Cart fetched",
                        cartService.getOrCreateCart(userDetails.getUsername())));
    }

    @PostMapping("/add")
    public ResponseEntity<ApiResponse<Cart>> addToCart(
            @Valid @RequestBody CartRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                ApiResponse.success("Item added",
                        cartService.addToCart(request, userDetails.getUsername())));
    }

    @DeleteMapping("/remove/{cartItemId}")
    public ResponseEntity<ApiResponse<Cart>> removeFromCart(
            @PathVariable Long cartItemId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
                ApiResponse.success("Item removed",
                        cartService.removeFromCart(cartItemId, userDetails.getUsername())));
    }

    @DeleteMapping("/clear")
    public ResponseEntity<ApiResponse<Void>> clearCart(
            @AuthenticationPrincipal UserDetails userDetails) {
        cartService.clearCart(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Cart cleared", null));
    }
}
