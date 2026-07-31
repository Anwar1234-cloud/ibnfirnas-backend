package com.ibnfirnas.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.math.BigDecimal;
import java.util.Map;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class ProductRequest {

    @NotBlank(message = "Product name is required")
    private String name;

    private String description;
    private String shortDescription;

    @NotNull(message = "Price is required")
    private BigDecimal price;

    private BigDecimal discountPrice;
    private String sku;
    private Integer stockQuantity;
    private Boolean isFeatured;
    private Boolean isActive;
    private Long categoryId;
    private String primaryImageUrl;
    private Map<String, String> specifications;
}