package com.smartpharma.service;

import com.smartpharma.dto.MlDailySale;
import com.smartpharma.dto.MlPredictRequest;
import com.smartpharma.dto.MlPredictionResult;
import com.smartpharma.dto.MlProductHistory;
import com.smartpharma.dto.OrderSuggestion;
import com.smartpharma.model.InventoryTransaction;
import com.smartpharma.model.Product;
import com.smartpharma.repository.InventoryTransactionRepository;
import com.smartpharma.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PredictionClient {

    private final ProductRepository productRepository;
    private final InventoryTransactionRepository transactionRepository;
    private final RestTemplate restTemplate;
    private final String mlServiceUrl;

    public PredictionClient(ProductRepository productRepository,
                             InventoryTransactionRepository transactionRepository,
                             RestTemplate restTemplate,
                             @Value("${ml.service.url}") String mlServiceUrl) {
        this.productRepository = productRepository;
        this.transactionRepository = transactionRepository;
        this.restTemplate = restTemplate;
        this.mlServiceUrl = mlServiceUrl;
    }

    public List<OrderSuggestion> getSuggestions() {
        List<Product> products = productRepository.findAll();
        if (products.isEmpty()) {
            return List.of();
        }

        List<MlProductHistory> histories = products.stream()
                .map(this::buildHistory)
                .collect(Collectors.toList());

        MlPredictionResult[] results = restTemplate.postForObject(
                mlServiceUrl + "/predict",
                new MlPredictRequest(histories),
                MlPredictionResult[].class
        );

        Map<Long, MlPredictionResult> resultsByProductId = new HashMap<>();
        if (results != null) {
            for (MlPredictionResult result : results) {
                resultsByProductId.put(result.getProductId(), result);
            }
        }

        List<OrderSuggestion> suggestions = new ArrayList<>();
        for (Product product : products) {
            MlPredictionResult result = resultsByProductId.get(product.getId());
            suggestions.add(new OrderSuggestion(
                    product.getId(),
                    product.getName(),
                    product.getBatchNumber(),
                    product.getCurrentQuantity(),
                    product.getMinThreshold(),
                    result != null ? result.getForecastedDemand30d() : 0,
                    result != null ? result.getSuggestedOrderQty() : 0,
                    result != null ? result.getConfidenceScore() : 0
            ));
        }
        return suggestions;
    }

    private MlProductHistory buildHistory(Product product) {
        List<InventoryTransaction> transactions = transactionRepository
                .findByProduct_IdAndReasonOrderByTransactionDateAsc(product.getId(), InventoryTransaction.Reason.SALE);

        Map<LocalDate, Integer> dailyTotals = transactions.stream()
                .collect(Collectors.groupingBy(
                        txn -> txn.getTransactionDate().toLocalDate(),
                        Collectors.summingInt(txn -> Math.abs(txn.getQuantityChange()))
                ));

        List<MlDailySale> dailySales = dailyTotals.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new MlDailySale(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());

        return new MlProductHistory(
                product.getId(),
                product.getName(),
                product.getCurrentQuantity(),
                product.getMinThreshold(),
                dailySales
        );
    }
}
