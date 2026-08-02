package com.smartpharma.service;

import com.smartpharma.dto.AnalyticsSummary;
import com.smartpharma.dto.ProductTotal;
import com.smartpharma.dto.ReasonCount;
import com.smartpharma.dto.TrendPoint;
import com.smartpharma.model.InventoryTransaction;
import com.smartpharma.repository.InventoryTransactionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

// Builds the three charts on the Analytics page, all from the same raw
// InventoryTransaction table - no separate aggregation tables or scheduled jobs.
@Service
public class AnalyticsService {

    public enum Period { WEEKLY, MONTHLY, YEARLY }

    private static final int TOP_PRODUCT_LIMIT = 8;
    private static final DateTimeFormatter MONTH_LABEL = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH);

    private final InventoryTransactionRepository transactionRepository;

    public AnalyticsService(InventoryTransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public AnalyticsSummary getSummary(Period period) {
        List<InventoryTransaction> transactions = transactionRepository.findAll();

        return new AnalyticsSummary(
                buildSalesTrend(transactions, period),
                buildTopProducts(transactions),
                buildReasonBreakdown(transactions)
        );
    }

    // Groups SALE transactions into week/month/year buckets and sums each one.
    // TreeMap keeps buckets sorted chronologically for free (bucketKey sorts as text).
    private List<TrendPoint> buildSalesTrend(List<InventoryTransaction> transactions, Period period) {
        Map<String, Integer> totalsByBucket = new TreeMap<>();

        for (InventoryTransaction txn : transactions) {
            if (txn.getReason() != InventoryTransaction.Reason.SALE) {
                continue;
            }
            LocalDate date = txn.getTransactionDate().toLocalDate();
            String bucketKey = bucketKey(date, period);
            totalsByBucket.merge(bucketKey, Math.abs(txn.getQuantityChange()), Integer::sum);
        }

        return totalsByBucket.entrySet().stream()
                .map(entry -> new TrendPoint(displayLabel(entry.getKey(), period), entry.getValue()))
                .collect(Collectors.toList());
    }

    private String bucketKey(LocalDate date, Period period) {
        if (period == Period.WEEKLY) {
            WeekFields weekFields = WeekFields.ISO;
            int week = date.get(weekFields.weekOfWeekBasedYear());
            int weekYear = date.get(weekFields.weekBasedYear());
            return String.format("%04d-W%02d", weekYear, week);
        }
        if (period == Period.YEARLY) {
            return String.valueOf(date.getYear());
        }
        return YearMonth.from(date).toString();
    }

    private String displayLabel(String bucketKey, Period period) {
        if (period == Period.WEEKLY || period == Period.YEARLY) {
            return bucketKey;
        }
        return YearMonth.parse(bucketKey).format(MONTH_LABEL);
    }

    // Ranks products by total units sold (all time) and keeps only the top N for the chart.
    private List<ProductTotal> buildTopProducts(List<InventoryTransaction> transactions) {
        Map<String, Integer> totalsByProduct = transactions.stream()
                .filter(txn -> txn.getReason() == InventoryTransaction.Reason.SALE)
                .collect(Collectors.groupingBy(
                        txn -> txn.getProduct().getName(),
                        Collectors.summingInt(txn -> Math.abs(txn.getQuantityChange()))
                ));

        return totalsByProduct.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(TOP_PRODUCT_LIMIT)
                .map(entry -> new ProductTotal(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    // Counts every transaction (SALE/RESTOCK/EXPIRED, not just sales) for the donut chart.
    private List<ReasonCount> buildReasonBreakdown(List<InventoryTransaction> transactions) {
        Map<InventoryTransaction.Reason, Long> countsByReason = transactions.stream()
                .collect(Collectors.groupingBy(InventoryTransaction::getReason, Collectors.counting()));

        return countsByReason.entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getKey().name()))
                .map(entry -> new ReasonCount(entry.getKey().name(), entry.getValue()))
                .collect(Collectors.toList());
    }
}
