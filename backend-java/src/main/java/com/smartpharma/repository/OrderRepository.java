package com.smartpharma.repository;

import com.smartpharma.model.Order;
import com.smartpharma.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    // Powers the Supplier's "Incoming Orders" list.
    List<Order> findBySupplierOrderByCreatedAtDesc(User supplier);

    // Powers the Manager's "Sent Orders" list.
    List<Order> findByCreatedByOrderByCreatedAtDesc(User createdBy);
}
