package com.smartpharma.controller;

import com.smartpharma.dto.CreateOrderRequest;
import com.smartpharma.dto.OrderResponse;
import com.smartpharma.dto.RespondToOrderRequest;
import com.smartpharma.model.User;
import com.smartpharma.repository.UserRepository;
import com.smartpharma.service.OrderService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Real, persisted order workflow between Managers and Suppliers.
 * (Distinct from OrdersController, which only exposes ML reorder suggestions.)
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final UserRepository userRepository;

    public OrderController(OrderService orderService, UserRepository userRepository) {
        this.orderService = orderService;
        this.userRepository = userRepository;
    }

    @PostMapping
    @PreAuthorize("hasRole('MANAGER')")
    public OrderResponse createOrder(@RequestBody CreateOrderRequest request, Authentication auth) {
        return orderService.createOrder(request, currentUser(auth));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'SUPPLIER')")
    public List<OrderResponse> getOrders(Authentication auth) {
        return orderService.getOrdersForUser(currentUser(auth));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'SUPPLIER')")
    public OrderResponse getOrder(@PathVariable Long id, Authentication auth) {
        return orderService.getOrderById(id, currentUser(auth));
    }

    @PostMapping("/{id}/respond")
    @PreAuthorize("hasRole('SUPPLIER')")
    public OrderResponse respondToOrder(@PathVariable Long id,
                                         @RequestBody RespondToOrderRequest request,
                                         Authentication auth) {
        return orderService.respondToOrder(id, request, currentUser(auth));
    }

    private User currentUser(Authentication auth) {
        return userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
