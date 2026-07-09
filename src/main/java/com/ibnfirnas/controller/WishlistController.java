package com.ibnfirnas.controller;

import com.ibnfirnas.dto.response.ApiResponse;
import com.ibnfirnas.dto.response.WishlistResponse;
import com.ibnfirnas.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<WishlistResponse>>> getWishlist(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success("Wishlist fetched",
                wishlistService.getWishlist(userDetails.getUsername())));
    }

    @PostMapping("/add/{productId}")
    public ResponseEntity<ApiResponse<WishlistResponse>> add(
            @PathVariable Long productId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success("Added to wishlist",
                wishlistService.addToWishlist(productId,
                        userDetails.getUsername())));
    }

    @DeleteMapping("/remove/{productId}")
    public ResponseEntity<ApiResponse<Void>> remove(
            @PathVariable Long productId,
            @AuthenticationPrincipal UserDetails userDetails) {
        wishlistService.removeFromWishlist(productId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(
                "Removed from wishlist", null));
    }

    @GetMapping("/check/{productId}")
    public ResponseEntity<ApiResponse<Boolean>> check(
            @PathVariable Long productId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success("Checked",
                wishlistService.isInWishlist(productId,
                        userDetails.getUsername())));
    }
}