package com.smartpharma.repository;

import com.smartpharma.model.Order;
import com.smartpharma.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findBySupplierOrderByCreatedAtDesc(User supplier);

    List<Order> findByCreatedByOrderByCreatedAtDesc(User createdBy);
}
