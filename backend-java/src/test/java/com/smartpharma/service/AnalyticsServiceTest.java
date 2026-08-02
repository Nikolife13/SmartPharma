package com.smartpharma.service;

import com.smartpharma.dto.AnalyticsSummary;
import com.smartpharma.dto.ProductTotal;
import com.smartpharma.dto.TrendPoint;
import com.smartpharma.model.InventoryTransaction;
import com.smartpharma.model.Product;
import com.smartpharma.repository.InventoryTransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the three Analytics chart aggregations - all computed in-memory
 * from a list of InventoryTransaction rows, no real database involved.
 */
@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private InventoryTransactionRepository transactionRepository;

    @InjectMocks
    private AnalyticsService analyticsService;

    private Product amoxicillin() {
        Product p = new Product();
        p.setId(1L);
        p.setName("Amoxicillin");
        return p;
    }

    private InventoryTransaction sale(Product product, int quantity, LocalDateTime date) {
        InventoryTransaction txn = new InventoryTransaction();
        txn.setProduct(product);
        txn.setReason(InventoryTransaction.Reason.SALE);
        txn.setQuantityChange(-quantity);
        txn.setTransactionDate(date);
        return txn;
    }

    @Test
    void getSummary_monthly_sumsSalesWithinTheSameCalendarMonth() {
        Product product = amoxicillin();
        when(transactionRepository.findAll()).thenReturn(List.of(
                sale(product, 5, LocalDateTime.of(2026, 3, 2, 12, 0)),
                sale(product, 7, LocalDateTime.of(2026, 3, 28, 12, 0)),
                sale(product, 3, LocalDateTime.of(2026, 4, 1, 12, 0))
        ));

        AnalyticsSummary summary = analyticsService.getSummary(AnalyticsService.Period.MONTHLY);

        assertThat(summary.getSalesTrend()).hasSize(2);
        TrendPoint march = summary.getSalesTrend().get(0);
        assertThat(march.getLabel()).isEqualTo("Mar 2026");
        assertThat(march.getTotalUnits()).isEqualTo(12);
    }

    @Test
    void getSummary_yearly_sumsSalesAcrossTheWholeYear() {
        Product product = amoxicillin();
        when(transactionRepository.findAll()).thenReturn(List.of(
                sale(product, 10, LocalDateTime.of(2025, 1, 5, 12, 0)),
                sale(product, 20, LocalDateTime.of(2025, 11, 20, 12, 0)),
                sale(product, 5, LocalDateTime.of(2026, 1, 5, 12, 0))
        ));

        AnalyticsSummary summary = analyticsService.getSummary(AnalyticsService.Period.YEARLY);

        assertThat(summary.getSalesTrend()).extracting(TrendPoint::getLabel).containsExactly("2025", "2026");
        assertThat(summary.getSalesTrend().get(0).getTotalUnits()).isEqualTo(30);
    }

    @Test
    void getSummary_weekly_groupsByIsoWeekNotCalendarMonth() {
        Product product = amoxicillin();
        // Both dates fall in the same ISO week (Mon 2026-03-02 .. Sun 2026-03-08).
        when(transactionRepository.findAll()).thenReturn(List.of(
                sale(product, 4, LocalDateTime.of(2026, 3, 2, 12, 0)),
                sale(product, 6, LocalDateTime.of(2026, 3, 8, 12, 0))
        ));

        AnalyticsSummary summary = analyticsService.getSummary(AnalyticsService.Period.WEEKLY);

        assertThat(summary.getSalesTrend()).hasSize(1);
        assertThat(summary.getSalesTrend().get(0).getTotalUnits()).isEqualTo(10);
    }

    @Test
    void getSummary_nonSaleTransactionsAreExcludedFromTheTrendChart() {
        Product product = amoxicillin();
        InventoryTransaction restock = new InventoryTransaction();
        restock.setProduct(product);
        restock.setReason(InventoryTransaction.Reason.RESTOCK);
        restock.setQuantityChange(50);
        restock.setTransactionDate(LocalDateTime.of(2026, 3, 2, 12, 0));

        when(transactionRepository.findAll()).thenReturn(List.of(restock));

        AnalyticsSummary summary = analyticsService.getSummary(AnalyticsService.Period.MONTHLY);

        assertThat(summary.getSalesTrend()).isEmpty();
    }

    @Test
    void getSummary_reasonBreakdown_countsEveryTransactionTypeIncludingNonSales() {
        Product product = amoxicillin();
        InventoryTransaction s = sale(product, 1, LocalDateTime.now());
        InventoryTransaction restock = new InventoryTransaction();
        restock.setProduct(product);
        restock.setReason(InventoryTransaction.Reason.RESTOCK);
        restock.setQuantityChange(10);
        restock.setTransactionDate(LocalDateTime.now());
        InventoryTransaction expired = new InventoryTransaction();
        expired.setProduct(product);
        expired.setReason(InventoryTransaction.Reason.EXPIRED);
        expired.setQuantityChange(-2);
        expired.setTransactionDate(LocalDateTime.now());

        when(transactionRepository.findAll()).thenReturn(List.of(s, restock, expired));

        AnalyticsSummary summary = analyticsService.getSummary(AnalyticsService.Period.MONTHLY);

        assertThat(summary.getReasonBreakdown()).hasSize(3);
        assertThat(summary.getReasonBreakdown())
                .extracting(rc -> rc.getReason())
                .containsExactlyInAnyOrder("SALE", "RESTOCK", "EXPIRED");
    }

    @Test
    void getSummary_topProducts_rankedByTotalUnitsSoldDescending() {
        Product a = amoxicillin();
        Product b = new Product();
        b.setId(2L);
        b.setName("Ibuprofen");

        when(transactionRepository.findAll()).thenReturn(List.of(
                sale(a, 5, LocalDateTime.now()),
                sale(b, 20, LocalDateTime.now())
        ));

        AnalyticsSummary summary = analyticsService.getSummary(AnalyticsService.Period.MONTHLY);

        assertThat(summary.getTopProducts()).extracting(ProductTotal::getProductName)
                .containsExactly("Ibuprofen", "Amoxicillin");
    }
}
