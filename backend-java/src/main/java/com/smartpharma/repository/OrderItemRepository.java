package com.smartpharma.repository;

import com.smartpharma.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

// No custom queries yet - items are always loaded/saved through their parent Order
// (see Order.items, cascade = ALL), this interface just gives Spring Data a bean to wire.
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
}
