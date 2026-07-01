package com.ibnfirnas.service;

import com.ibnfirnas.dto.response.WishlistResponse;
import com.ibnfirnas.entity.*;
import com.ibnfirnas.exception.BadRequestException;
import com.ibnfirnas.exception.ResourceNotFoundException;
import com.ibnfirnas.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    // ============ toDTO ============
    public WishlistResponse toDTO(Wishlist wishlist) {
        if (wishlist == null) return null;

        Product product = wishlist.getProduct();
        String imageUrl = null;
        if (product.getImages() != null && !product.getImages().isEmpty()) {
            imageUrl = product.getImages().stream()
                    .filter(img -> img.getIsPrimary() != null && img.getIsPrimary())
                    .findFirst()
                    .map(img -> img.getImageUrl())
                    .orElse(product.getImages().get(0).getImageUrl());
        }

        return WishlistResponse.builder()
                .id(wishlist.getId())
                .productId(product.getId())
                .productName(product.getName())
                .productSlug(product.getSlug())
                .productPrice(product.getPrice())
                .productDiscountPrice(product.getDiscountPrice())
                .productImage(imageUrl)
                .addedAt(wishlist.getCreatedAt())
                .build();
    }

    // ============ CRUD ============
    public List<WishlistResponse> getWishlist(String email) {
        User user = getUser(email);
        return wishlistRepository.findByUserId(user.getId())
                .stream().map(this::toDTO)
                .collect(Collectors.toList());
    }

    public WishlistResponse addToWishlist(Long productId, String email) {
        User user = getUser(email);

        if (wishlistRepository.existsByUserIdAndProductId(
                user.getId(), productId)) {
            throw new BadRequestException("Product already in wishlist");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found with id: " + productId));

        Wishlist wishlist = wishlistRepository.save(
                Wishlist.builder().user(user).product(product).build());

        return toDTO(wishlist);
    }

    @Transactional
    public void removeFromWishlist(Long productId, String email) {
        User user = getUser(email);
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException(
                    "Product not found with id: " + productId);
        }
        wishlistRepository.deleteByUserIdAndProductId(user.getId(), productId);
    }

    public boolean isInWishlist(Long productId, String email) {
        User user = getUser(email);
        return wishlistRepository.existsByUserIdAndProductId(
                user.getId(), productId);
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with email: " + email));
    }
}