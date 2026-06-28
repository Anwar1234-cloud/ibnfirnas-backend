package com.ibnfirnas.dto.response;


import java.math.BigDecimal;
import java.time.LocalDateTime;

public record WishlistResponse(
        Long id,
        Long productId,
        String productName,
        String productImageUrl,
        BigDecimal productPrice,
        LocalDateTime addedAt
) {


}