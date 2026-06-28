package com.ibnfirnas.controller;

import com.ibnfirnas.dto.request.ReviewRequest;
import com.ibnfirnas.dto.response.ApiResponse;
import com.ibnfirnas.entity.ProductReview;
import com.ibnfirnas.service.ProductReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ProductReviewController {

    private final ProductReviewService reviewService;

    @GetMapping("/product/{productId}")
    public ResponseEntity<ApiResponse<List<ProductReview>>> getByProduct(
            @PathVariable Long productId) {
        return ResponseEntity.ok(ApiResponse.success("Reviews fetched",
                reviewService.getReviewsByProduct(productId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProductReview>> addReview(
            @Valid @RequestBody ReviewRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success("Review added",
                reviewService.addReview(request, userDetails.getUsername())));
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<ApiResponse<Void>> deleteReview(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal UserDetails userDetails) {
        reviewService.deleteReview(reviewId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Review deleted", null));
    }
}