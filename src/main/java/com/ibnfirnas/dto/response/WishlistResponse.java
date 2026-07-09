package com.ibnfirnas.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class WishlistResponse {
    private Long id;
    private Long productId;
    private String productName;
    private String productSlug;
    private BigDecimal productPrice;
    private BigDecimal productDiscountPrice;
    private String productImage;
    private LocalDateTime addedAt;
}