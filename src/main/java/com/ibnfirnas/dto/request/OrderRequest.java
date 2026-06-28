package com.ibnfirnas.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class OrderRequest {

    @NotNull(message = "Items are required")
    private List<OrderItemRequest> items;

    private String shippingAddress;
    private String paymentMethod;
    private String notes;

    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor
    public static class OrderItemRequest {
        private Long productId;
        private Integer quantity;
    }
}
