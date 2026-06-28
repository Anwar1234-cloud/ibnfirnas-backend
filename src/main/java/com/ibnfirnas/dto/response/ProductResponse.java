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
    private Boolean isActive;
    private String categoryName;
    private String primaryImageUrl;
    private LocalDateTime createdAt;
}