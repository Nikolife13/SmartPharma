package com.smartpharma.repository;

import com.smartpharma.model.InventoryTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Long> {

    // Used by PredictionClient to build the day-by-day sales history sent to the ML service.
    List<InventoryTransaction> findByProduct_IdAndReasonOrderByTransactionDateAsc(
            Long productId, InventoryTransaction.Reason reason);
}
