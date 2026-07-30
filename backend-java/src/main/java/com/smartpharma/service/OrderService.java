package com.smartpharma.service;

import com.smartpharma.dto.CreateOrderRequest;
import com.smartpharma.dto.OrderResponse;
import com.smartpharma.dto.RespondToOrderRequest;
import com.smartpharma.model.Order;
import com.smartpharma.model.OrderItem;
import com.smartpharma.model.Product;
import com.smartpharma.model.User;
import com.smartpharma.repository.OrderRepository;
import com.smartpharma.repository.ProductRepository;
import com.smartpharma.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    public OrderService(OrderRepository orderRepository,
                         ProductRepository productRepository,
                         UserRepository userRepository,
                         EmailService emailService) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request, User manager) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new RuntimeException("Order must contain at least one item");
        }

        User supplier = userRepository.findById(request.getSupplierId())
                .filter(u -> u.getRole() == User.Role.SUPPLIER)
                .orElseThrow(() -> new RuntimeException("Supplier not found"));
        if (supplier.getSupplierStatus() != User.SupplierStatus.ACTIVE) {
            throw new RuntimeException("Supplier is not active");
        }

        Order order = new Order();
        order.setSupplier(supplier);
        order.setCreatedBy(manager);
        order.setStatus(Order.Status.PENDING);

        for (CreateOrderRequest.OrderLineRequest line : request.getItems()) {
            Product product = productRepository.findById(line.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found"));

            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProduct(product);
            item.setProductName(product.getName());
            item.setRequestedQty(line.getQuantity());
            item.setStatus(OrderItem.Status.PENDING);
            order.getItems().add(item);
        }

        Order saved = orderRepository.save(order);
        emailService.sendOrderNotification(saved);
        return toResponse(saved);
    }

    public List<OrderResponse> getOrdersForUser(User requester) {
        List<Order> orders = switch (requester.getRole()) {
            case MANAGER -> orderRepository.findByCreatedByOrderByCreatedAtDesc(requester);
            case SUPPLIER -> orderRepository.findBySupplierOrderByCreatedAtDesc(requester);
            default -> throw new RuntimeException("Not authorized");
        };
        return orders.stream().map(this::toResponse).toList();
    }

    public OrderResponse getOrderById(Long id, User requester) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        assertCanView(order, requester);
        return toResponse(order);
    }

    @Transactional
    public OrderResponse respondToOrder(Long orderId, RespondToOrderRequest request, User supplier) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!order.getSupplier().getId().equals(supplier.getId())) {
            throw new RuntimeException("Not authorized");
        }
        if (supplier.getSupplierStatus() != User.SupplierStatus.ACTIVE) {
            throw new RuntimeException("Supplier account is not active");
        }
        if (order.getStatus() != Order.Status.PENDING) {
            throw new RuntimeException("Order has already been responded to");
        }

        Map<Long, OrderItem> itemsById = order.getItems().stream()
                .collect(Collectors.toMap(OrderItem::getId, Function.identity()));

        for (RespondToOrderRequest.ItemResponse update : request.getItems()) {
            OrderItem item = itemsById.get(update.getItemId());
            if (item == null) {
                throw new RuntimeException("Item not in this order");
            }
            if (update.isAvailable()) {
                item.setStatus(OrderItem.Status.AVAILABLE);
                item.setConfirmedQty(update.getConfirmedQty() != null ? update.getConfirmedQty() : item.getRequestedQty());
            } else {
                item.setStatus(OrderItem.Status.UNAVAILABLE);
                item.setConfirmedQty(0);
            }
        }

        order.setExpectedDeliveryDate(request.getExpectedDeliveryDate());
        order.setSupplierNote(request.getSupplierNote());

        boolean allAvailable = order.getItems().stream().allMatch(i -> i.getStatus() == OrderItem.Status.AVAILABLE);
        boolean anyAvailable = order.getItems().stream().anyMatch(i -> i.getStatus() == OrderItem.Status.AVAILABLE);
        order.setStatus(allAvailable ? Order.Status.APPROVED
                : anyAvailable ? Order.Status.PARTIALLY_APPROVED
                : Order.Status.REJECTED);

        Order saved = orderRepository.save(order);
        return toResponse(saved);
    }

    private void assertCanView(Order order, User requester) {
        boolean isOwningManager = requester.getRole() == User.Role.MANAGER
                && order.getCreatedBy().getId().equals(requester.getId());
        boolean isOwningSupplier = requester.getRole() == User.Role.SUPPLIER
                && order.getSupplier().getId().equals(requester.getId());
        if (!isOwningManager && !isOwningSupplier) {
            throw new RuntimeException("Not authorized");
        }
    }

    private OrderResponse toResponse(Order order) {
        List<OrderResponse.OrderItemResponse> items = order.getItems().stream()
                .map(i -> new OrderResponse.OrderItemResponse(
                        i.getId(),
                        i.getProduct().getId(),
                        i.getProductName(),
                        i.getRequestedQty(),
                        i.getStatus().name(),
                        i.getConfirmedQty()))
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getSupplier().getId(),
                order.getSupplier().getUsername(),
                order.getCreatedBy().getId(),
                order.getCreatedBy().getUsername(),
                order.getStatus().name(),
                order.getExpectedDeliveryDate(),
                order.getSupplierNote(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                items
        );
    }
}
