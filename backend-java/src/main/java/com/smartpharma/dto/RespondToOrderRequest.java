package com.smartpharma.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class RespondToOrderRequest {
    private List<ItemResponse> items;
    private LocalDate expectedDeliveryDate;
    private String supplierNote;

    @Data
    public static class ItemResponse {
        private Long itemId;
        private boolean available;
        private Integer confirmedQty;
    }
}
