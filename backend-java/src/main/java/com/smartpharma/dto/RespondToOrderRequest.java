package com.smartpharma.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

// Body of POST /api/orders/{id}/respond - the supplier confirming what they can
// actually deliver, item by item, plus a single delivery date for the whole order.
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
