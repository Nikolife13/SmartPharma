package com.smartpharma.repository;

import com.smartpharma.model.InventoryTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Long> {

    List<InventoryTransaction> findByProduct_IdAndReasonOrderByTransactionDateAsc(
            Long productId, InventoryTransaction.Reason reason);
}
