package com.ibnfirnas.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class ReviewRequest {

    @NotNull(message = "Product ID is required")
    private Long productId;

    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating minimum 1")
    @Max(value = 5, message = "Rating maximum 5")
    private Integer rating;

    private String title;
    private String comment;
}