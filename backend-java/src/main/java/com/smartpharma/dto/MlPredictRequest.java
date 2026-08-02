package com.smartpharma.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
// Full request body sent to the ML service: one forecast call for every product at once.
public class MlPredictRequest {
    private List<MlProductHistory> products;
}
