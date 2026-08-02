package com.smartpharma.dto;

import lombok.Data;

import java.util.List;

// Body of POST /api/orders - a Manager picking products/quantities to send to one supplier.
@Data
public class CreateOrderRequest {
    private Long supplierId;
    private List<OrderLineRequest> items;

    @Data
    public static class OrderLineRequest {
        private Long productId;
        private Integer quantity;
    }
}
