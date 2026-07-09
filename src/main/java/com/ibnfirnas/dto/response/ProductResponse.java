package com.ibnfirnas.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ProductResponse {
    private Long id;
    private String name;
    private String slug;
    private String description;
    private String shortDescription;
    private BigDecimal price;
    private BigDecimal discountPrice;
    private String sku;
    private Integer stockQuantity;
    private String stockStatus;
    private Boolean isFeatured;
    private Long categoryId;
    private Boolean isActive;
    private String categoryName;
    private Double averageRating;
    private Integer totalReviews;
    private String primaryImageUrl;
    private LocalDateTime createdAt;
}