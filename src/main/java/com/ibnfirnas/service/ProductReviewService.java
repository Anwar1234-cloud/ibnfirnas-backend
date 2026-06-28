package com.ibnfirnas.service;

import com.ibnfirnas.dto.request.ReviewRequest;
import com.ibnfirnas.entity.*;
import com.ibnfirnas.exception.BadRequestException;
import com.ibnfirnas.exception.ResourceNotFoundException;
import com.ibnfirnas.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductReviewService {

    private final ProductReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public List<ProductReview> getReviewsByProduct(Long productId) {
        return reviewRepository.findByProductId(productId);
    }

    public ProductReview addReview(ReviewRequest request, String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (reviewRepository.existsByUserIdAndProductId(user.getId(), request.getProductId())) {
            throw new BadRequestException("You have already reviewed this product");
        }

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        ProductReview review = ProductReview.builder()
                .user(user)
                .product(product)
                .rating(request.getRating())
                .title(request.getTitle())
                .comment(request.getComment())
                .build();

        ProductReview saved = reviewRepository.save(review);

        // Update product average rating
        Double avg = reviewRepository.getAverageRatingByProductId(product.getId());
        product.setAverageRating(avg != null ? avg : 0.0);
        productRepository.save(product);

        return saved;
    }

    public void deleteReview(Long reviewId, String email) {
        ProductReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));
        if (!review.getUser().getEmail().equals(email)) {
            throw new BadRequestException("You can only delete your own review");
        }
        reviewRepository.deleteById(reviewId);
    }
}