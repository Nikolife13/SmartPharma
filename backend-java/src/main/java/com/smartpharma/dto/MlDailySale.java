package com.smartpharma.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
// One day's worth of units sold, as sent to the FastAPI ML service.
public class MlDailySale {
    private LocalDate date;
    private Integer quantity;
}
