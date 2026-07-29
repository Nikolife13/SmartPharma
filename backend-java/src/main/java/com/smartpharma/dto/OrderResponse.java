package com.smartpharma.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    private Long id;
    private Long supplierId;
    private String supplierUsername;
    private Long createdById;
    private String createdByUsername;
    private String status;
    private LocalDate expectedDeliveryDate;
    private String supplierNote;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<OrderItemResponse> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemResponse {
        private Long id;
        private Long productId;
        private String productName;
        private Integer requestedQty;
        private String status;
        private Integer confirmedQty;
    }
}
