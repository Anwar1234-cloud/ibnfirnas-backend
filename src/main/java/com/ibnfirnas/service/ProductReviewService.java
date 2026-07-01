package com.ibnfirnas.service;

import com.ibnfirnas.dto.request.ReviewRequest;
import com.ibnfirnas.dto.response.ReviewResponse;
import com.ibnfirnas.entity.*;
import com.ibnfirnas.exception.BadRequestException;
import com.ibnfirnas.exception.ResourceNotFoundException;
import com.ibnfirnas.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductReviewService {

    private final ProductReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    // ============ toDTO ============
    public ReviewResponse toDTO(ProductReview review) {
        if (review == null) return null;
        return ReviewResponse.builder()
                .id(review.getId())
                .productId(review.getProduct() != null
                        ? review.getProduct().getId() : null)
                .userFullName(review.getUser() != null
                        ? review.getUser().getFullName() : null)
                .userAvatar(review.getUser() != null
                        ? review.getUser().getAvatarUrl() : null)
                .rating(review.getRating())
                .title(review.getTitle())
                .comment(review.getComment())
                .isVerified(review.getIsVerified())
                .createdAt(review.getCreatedAt())
                .build();
    }

    // ============ CRUD ============
    public List<ReviewResponse> getReviewsByProduct(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException(
                    "Product not found with id: " + productId);
        }
        return reviewRepository.findByProductId(productId)
                .stream().map(this::toDTO)
                .collect(Collectors.toList());
    }

    public ReviewResponse addReview(ReviewRequest request, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found: " + email));

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Product not found: " + request.getProductId()));

        if (reviewRepository.existsByUserIdAndProductId(
                user.getId(), product.getId())) {
            throw new BadRequestException(
                    "You have already reviewed this product");
        }

        if (request.getRating() < 1 || request.getRating() > 5) {
            throw new BadRequestException("Rating must be between 1 and 5");
        }

        ProductReview review = ProductReview.builder()
                .user(user)
                .product(product)
                .rating(request.getRating())
                .title(request.getTitle())
                .comment(request.getComment())
                .build();

        ProductReview saved = reviewRepository.save(review);

        Double avg = reviewRepository.getAverageRatingByProductId(product.getId());
        product.setAverageRating(avg != null ? avg : 0.0);
        product.setTotalReviews(reviewRepository.findByProductId(
                product.getId()).size());
        productRepository.save(product);

        return toDTO(saved);
    }

    public void deleteReview(Long reviewId, String email) {
        ProductReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Review not found with id: " + reviewId));

        if (!review.getUser().getEmail().equals(email)) {
            throw new BadRequestException(
                    "You can only delete your own review");
        }
        reviewRepository.deleteById(reviewId);
    }
}