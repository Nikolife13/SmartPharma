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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the core supplier-order workflow: creating an order and the
 * supplier responding to it. Repositories/EmailService are mocked - no database,
 * no Spring context - these run in milliseconds.
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private EmailService emailService;

    @InjectMocks
    private OrderService orderService;

    private User manager;
    private User supplier;
    private Product product;

    @BeforeEach
    void setUp() {
        manager = new User();
        manager.setId(1L);
        manager.setUsername("manager1");
        manager.setRole(User.Role.MANAGER);

        supplier = new User();
        supplier.setId(2L);
        supplier.setUsername("supplier1");
        supplier.setRole(User.Role.SUPPLIER);
        supplier.setSupplierStatus(User.SupplierStatus.ACTIVE);

        product = new Product();
        product.setId(10L);
        product.setName("Amoxicillin");
        product.setMinThreshold(20);
        product.setCurrentQuantity(5);
    }

    // --- createOrder --------------------------------------------------------

    @Test
    void createOrder_withNoItems_throws() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setSupplierId(2L);
        request.setItems(List.of());

        assertThatThrownBy(() -> orderService.createOrder(request, manager))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Order must contain at least one item");
    }

    @Test
    void createOrder_supplierNotActive_throws() {
        supplier.setSupplierStatus(User.SupplierStatus.PENDING);
        when(userRepository.findById(2L)).thenReturn(Optional.of(supplier));

        CreateOrderRequest request = validCreateRequest();

        assertThatThrownBy(() -> orderService.createOrder(request, manager))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Supplier is not active");
    }

    @Test
    void createOrder_validRequest_savesOrderWithPendingStatusAndNotifiesSupplier() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(supplier));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = orderService.createOrder(validCreateRequest(), manager);

        assertThat(response.getStatus()).isEqualTo("PENDING");
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getProductName()).isEqualTo("Amoxicillin");
    }

    // --- respondToOrder: status derivation ----------------------------------

    @Test
    void respondToOrder_allItemsAvailable_orderBecomesApproved() {
        Order order = pendingOrderWithOneItem();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RespondToOrderRequest request = respondRequest(order, true, 30);
        OrderResponse response = orderService.respondToOrder(1L, request, supplier);

        assertThat(response.getStatus()).isEqualTo("APPROVED");
        assertThat(response.getItems().get(0).getStatus()).isEqualTo("AVAILABLE");
    }

    @Test
    void respondToOrder_someItemsUnavailable_orderBecomesPartiallyApproved() {
        Order order = pendingOrderWithTwoItems();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RespondToOrderRequest.ItemResponse first = new RespondToOrderRequest.ItemResponse();
        first.setItemId(order.getItems().get(0).getId());
        first.setAvailable(true);
        first.setConfirmedQty(30);

        RespondToOrderRequest.ItemResponse second = new RespondToOrderRequest.ItemResponse();
        second.setItemId(order.getItems().get(1).getId());
        second.setAvailable(false);

        RespondToOrderRequest request = new RespondToOrderRequest();
        request.setItems(List.of(first, second));

        OrderResponse response = orderService.respondToOrder(1L, request, supplier);

        assertThat(response.getStatus()).isEqualTo("PARTIALLY_APPROVED");
    }

    @Test
    void respondToOrder_noItemsAvailable_orderBecomesRejected() {
        Order order = pendingOrderWithOneItem();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RespondToOrderRequest request = respondRequest(order, false, null);
        OrderResponse response = orderService.respondToOrder(1L, request, supplier);

        assertThat(response.getStatus()).isEqualTo("REJECTED");
        assertThat(response.getItems().get(0).getConfirmedQty()).isEqualTo(0);
    }

    @Test
    void respondToOrder_unavailableItem_defaultsConfirmedQtyToRequestedWhenNotSpecified() {
        Order order = pendingOrderWithOneItem();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // available=true but no confirmedQty given -> should fall back to requestedQty (30)
        RespondToOrderRequest request = respondRequest(order, true, null);
        OrderResponse response = orderService.respondToOrder(1L, request, supplier);

        assertThat(response.getItems().get(0).getConfirmedQty()).isEqualTo(30);
    }

    // --- respondToOrder: authorization/state guards -------------------------

    @Test
    void respondToOrder_bySomeoneElsesSupplier_throwsNotAuthorized() {
        Order order = pendingOrderWithOneItem();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        User otherSupplier = new User();
        otherSupplier.setId(99L);
        otherSupplier.setRole(User.Role.SUPPLIER);
        otherSupplier.setSupplierStatus(User.SupplierStatus.ACTIVE);

        RespondToOrderRequest request = respondRequest(order, true, 30);

        assertThatThrownBy(() -> orderService.respondToOrder(1L, request, otherSupplier))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Not authorized");
    }

    @Test
    void respondToOrder_supplierNoLongerActive_throws() {
        Order order = pendingOrderWithOneItem();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        supplier.setSupplierStatus(User.SupplierStatus.REJECTED);

        RespondToOrderRequest request = respondRequest(order, true, 30);

        assertThatThrownBy(() -> orderService.respondToOrder(1L, request, supplier))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Supplier account is not active");
    }

    @Test
    void respondToOrder_alreadyRespondedTo_throws() {
        Order order = pendingOrderWithOneItem();
        order.setStatus(Order.Status.APPROVED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        RespondToOrderRequest request = respondRequest(order, true, 30);

        assertThatThrownBy(() -> orderService.respondToOrder(1L, request, supplier))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Order has already been responded to");
    }

    // --- getOrderById: view authorization ------------------------------------

    @Test
    void getOrderById_byUnrelatedManager_throwsNotAuthorized() {
        Order order = pendingOrderWithOneItem();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        User otherManager = new User();
        otherManager.setId(77L);
        otherManager.setRole(User.Role.MANAGER);

        assertThatThrownBy(() -> orderService.getOrderById(1L, otherManager))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Not authorized");
    }

    @Test
    void getOrderById_byOwningSupplier_succeeds() {
        Order order = pendingOrderWithOneItem();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        OrderResponse response = orderService.getOrderById(1L, supplier);

        assertThat(response.getId()).isEqualTo(1L);
    }

    // --- helpers --------------------------------------------------------------

    private CreateOrderRequest validCreateRequest() {
        CreateOrderRequest.OrderLineRequest line = new CreateOrderRequest.OrderLineRequest();
        line.setProductId(10L);
        line.setQuantity(30);

        CreateOrderRequest request = new CreateOrderRequest();
        request.setSupplierId(2L);
        request.setItems(List.of(line));
        return request;
    }

    private Order pendingOrderWithOneItem() {
        Order order = new Order();
        order.setId(1L);
        order.setSupplier(supplier);
        order.setCreatedBy(manager);
        order.setStatus(Order.Status.PENDING);

        OrderItem item = new OrderItem();
        item.setId(100L);
        item.setOrder(order);
        item.setProduct(product);
        item.setProductName(product.getName());
        item.setRequestedQty(30);
        item.setStatus(OrderItem.Status.PENDING);

        order.setItems(new java.util.ArrayList<>(List.of(item)));
        return order;
    }

    private Order pendingOrderWithTwoItems() {
        Order order = pendingOrderWithOneItem();

        OrderItem second = new OrderItem();
        second.setId(101L);
        second.setOrder(order);
        second.setProduct(product);
        second.setProductName("Ibuprofen");
        second.setRequestedQty(10);
        second.setStatus(OrderItem.Status.PENDING);

        order.getItems().add(second);
        return order;
    }

    private RespondToOrderRequest respondRequest(Order order, boolean available, Integer confirmedQty) {
        RespondToOrderRequest.ItemResponse itemResponse = new RespondToOrderRequest.ItemResponse();
        itemResponse.setItemId(order.getItems().get(0).getId());
        itemResponse.setAvailable(available);
        itemResponse.setConfirmedQty(confirmedQty);

        RespondToOrderRequest request = new RespondToOrderRequest();
        request.setItems(List.of(itemResponse));
        return request;
    }
}
