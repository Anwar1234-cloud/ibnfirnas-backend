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

@Service
@RequiredArgsConstructor
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public List<Wishlist> getWishlist(String email) {
        User user = getUser(email);
        return wishlistRepository.findByUserId(user.getId());
    }

    @Transactional
    public Wishlist addToWishlist(Long productId, String email) {
        User user = getUser(email);
        if (wishlistRepository.existsByUserIdAndProductId(user.getId(), productId)) {
            throw new BadRequestException("Product already in wishlist");
        }
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        return wishlistRepository.save(
                Wishlist.builder().user(user).product(product).build());
    }

    @Transactional
    public void removeFromWishlist(Long productId, String email) {
        User user = getUser(email);
        wishlistRepository.deleteByUserIdAndProductId(user.getId(), productId);
    }

    public boolean isInWishlist(Long productId, String email) {
        User user = getUser(email);
        return wishlistRepository.existsByUserIdAndProductId(user.getId(), productId);
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }


    // Add this private method inside WishlistService

    private WishlistResponse toResponse(Wishlist wishlist) {
        return WishlistResponse.builder()
                .id(wishlist.getId())
                .productId(wishlist.getProduct().getId())
                .productName(wishlist.getProduct().getName())
                .productImageUrl(wishlist.getProduct().getImageUrl())
                .productPrice(wishlist.getProduct().getPrice())
                .addedAt(wishlist.getCreatedAt())
                .build();
    }
}