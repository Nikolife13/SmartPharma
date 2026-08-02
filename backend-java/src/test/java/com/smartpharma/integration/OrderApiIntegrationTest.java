package com.smartpharma.integration;

import com.smartpharma.dto.*;
import com.smartpharma.model.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end integration tests for the REST API: real HTTP requests over a
 * real (randomly-assigned) port, through the real Spring Security filter
 * chain and real JWT issuance/validation, against an in-memory H2 database
 * (see src/test/resources/application.properties). Unlike the service-layer
 * unit tests, these verify that role restrictions are actually enforced at
 * the HTTP boundary - not just that the Java code would do the right thing
 * if called directly.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderApiIntegrationTest {

    @LocalServerPort
    private int port;

    private TestRestTemplate rest;

    @BeforeEach
    void setUp() {
        // The default JDK HttpURLConnection-based client used by TestRestTemplate
        // doesn't support PATCH (throws "Invalid HTTP method: PATCH") - swap in the
        // java.net.http.HttpClient-backed factory, which handles it natively.
        rest = new TestRestTemplate();
        rest.getRestTemplate().setRequestFactory(new JdkClientHttpRequestFactory());
    }

    private String baseUrl(String path) {
        return "http://localhost:" + port + path;
    }

    private HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return headers;
    }

    private String register(String username, String password, String role, String email) {
        RegisterRequest request = new RegisterRequest();
        request.setUsername(username);
        request.setPassword(password);
        request.setRole(role);
        request.setEmail(email);

        ResponseEntity<LoginResponse> response = rest.postForEntity(
                baseUrl("/api/auth/register"), request, LoginResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody().getToken();
    }

    @Test
    void unauthenticatedRequest_isRejected() {
        ResponseEntity<String> response = rest.getForEntity(baseUrl("/api/orders"), String.class);

        assertThat(response.getStatusCode()).isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
    }

    @Test
    void supplierCannotCreateAnOrder() {
        register("mgr-perm-check", "password123", "MANAGER", null);
        String supplierToken = register("sup-perm-check", "password123", "SUPPLIER", "s@example.com");

        CreateOrderRequest orderRequest = new CreateOrderRequest();
        orderRequest.setSupplierId(1L);
        orderRequest.setItems(List.of());

        ResponseEntity<String> response = rest.exchange(
                baseUrl("/api/orders"), HttpMethod.POST,
                new HttpEntity<>(orderRequest, authHeaders(supplierToken)), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void pendingSupplier_cannotBeOrderedFrom_untilAManagerApprovesThem() {
        String managerToken = register("mgr-approval-flow", "password123", "MANAGER", null);
        register("sup-approval-flow", "password123", "SUPPLIER", "sup-approval-flow@example.com");

        // Find the pending supplier via the admin endpoint.
        ResponseEntity<SupplierSummary[]> pending = rest.exchange(
                baseUrl("/api/admin/suppliers?status=PENDING"), HttpMethod.GET,
                new HttpEntity<>(authHeaders(managerToken)), SupplierSummary[].class);
        assertThat(pending.getStatusCode()).isEqualTo(HttpStatus.OK);
        Long supplierId = List.of(pending.getBody()).stream()
                .filter(s -> s.getUsername().equals("sup-approval-flow"))
                .findFirst().orElseThrow().getId();

        // A product to order, created by the manager.
        Long productId = createProduct(managerToken, "Test Drug A");

        // Ordering from a still-pending supplier must fail.
        CreateOrderRequest orderRequest = new CreateOrderRequest();
        orderRequest.setSupplierId(supplierId);
        CreateOrderRequest.OrderLineRequest line = new CreateOrderRequest.OrderLineRequest();
        line.setProductId(productId);
        line.setQuantity(20);
        orderRequest.setItems(List.of(line));

        ResponseEntity<String> beforeApproval = rest.exchange(
                baseUrl("/api/orders"), HttpMethod.POST,
                new HttpEntity<>(orderRequest, authHeaders(managerToken)), String.class);
        assertThat(beforeApproval.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        // Manager approves the supplier.
        UpdateSupplierStatusRequest statusRequest = new UpdateSupplierStatusRequest();
        statusRequest.setStatus("ACTIVE");
        ResponseEntity<SupplierSummary> approve = rest.exchange(
                baseUrl("/api/admin/suppliers/" + supplierId + "/status"), HttpMethod.PATCH,
                new HttpEntity<>(statusRequest, authHeaders(managerToken)), SupplierSummary.class);
        assertThat(approve.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(approve.getBody().getSupplierStatus()).isEqualTo("ACTIVE");

        // Same order now succeeds.
        ResponseEntity<OrderResponse> afterApproval = rest.exchange(
                baseUrl("/api/orders"), HttpMethod.POST,
                new HttpEntity<>(orderRequest, authHeaders(managerToken)), OrderResponse.class);
        assertThat(afterApproval.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(afterApproval.getBody().getStatus()).isEqualTo("PENDING");
    }

    @Test
    void fullOrderLifecycle_managerSendsSupplierResponds() {
        String managerToken = register("mgr-lifecycle", "password123", "MANAGER", null);
        String supplierToken = register("sup-lifecycle", "password123", "SUPPLIER", "sup-lifecycle@example.com");

        Long supplierId = activateSupplier(managerToken, "sup-lifecycle");
        Long productId = createProduct(managerToken, "Test Drug B");

        // Manager sends the order.
        CreateOrderRequest orderRequest = new CreateOrderRequest();
        orderRequest.setSupplierId(supplierId);
        CreateOrderRequest.OrderLineRequest line = new CreateOrderRequest.OrderLineRequest();
        line.setProductId(productId);
        line.setQuantity(15);
        orderRequest.setItems(List.of(line));

        ResponseEntity<OrderResponse> created = rest.exchange(
                baseUrl("/api/orders"), HttpMethod.POST,
                new HttpEntity<>(orderRequest, authHeaders(managerToken)), OrderResponse.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.OK);
        Long orderId = created.getBody().getId();
        Long itemId = created.getBody().getItems().get(0).getId();

        // Supplier sees it in their inbox.
        ResponseEntity<OrderResponse[]> supplierInbox = rest.exchange(
                baseUrl("/api/orders"), HttpMethod.GET,
                new HttpEntity<>(authHeaders(supplierToken)), OrderResponse[].class);
        assertThat(supplierInbox.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(List.of(supplierInbox.getBody())).anyMatch(o -> o.getId().equals(orderId));

        // Supplier confirms availability.
        RespondToOrderRequest.ItemResponse itemResponse = new RespondToOrderRequest.ItemResponse();
        itemResponse.setItemId(itemId);
        itemResponse.setAvailable(true);
        itemResponse.setConfirmedQty(15);
        RespondToOrderRequest respondRequest = new RespondToOrderRequest();
        respondRequest.setItems(List.of(itemResponse));
        respondRequest.setExpectedDeliveryDate(LocalDate.now().plusDays(5));

        ResponseEntity<OrderResponse> responded = rest.exchange(
                baseUrl("/api/orders/" + orderId + "/respond"), HttpMethod.POST,
                new HttpEntity<>(respondRequest, authHeaders(supplierToken)), OrderResponse.class);
        assertThat(responded.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responded.getBody().getStatus()).isEqualTo("APPROVED");

        // A different manager cannot see this order (ownership check).
        String otherManagerToken = register("mgr-outsider", "password123", "MANAGER", null);
        ResponseEntity<String> forbiddenView = rest.exchange(
                baseUrl("/api/orders/" + orderId), HttpMethod.GET,
                new HttpEntity<>(authHeaders(otherManagerToken)), String.class);
        assertThat(forbiddenView.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void supplierCannotAccessInternalInventoryEndpoints() {
        String supplierToken = register("sup-inventory-block", "password123", "SUPPLIER", "s2@example.com");

        ResponseEntity<String> products = rest.exchange(
                baseUrl("/api/products"), HttpMethod.GET,
                new HttpEntity<>(authHeaders(supplierToken)), String.class);
        ResponseEntity<String> dashboard = rest.exchange(
                baseUrl("/api/dashboard"), HttpMethod.GET,
                new HttpEntity<>(authHeaders(supplierToken)), String.class);

        assertThat(products.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(dashboard.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    private Long createProduct(String managerToken, String name) {
        ProductRequest request = new ProductRequest();
        request.setName(name);
        request.setBatchNumber("BATCH-IT-" + System.nanoTime());
        request.setExpiryDate(LocalDate.now().plusYears(1));
        request.setMinThreshold(10);
        request.setCurrentQuantity(5);

        ResponseEntity<Product> response = rest.exchange(
                baseUrl("/api/products"), HttpMethod.POST,
                new HttpEntity<>(request, authHeaders(managerToken)), Product.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody().getId();
    }

    private Long activateSupplier(String managerToken, String supplierUsername) {
        ResponseEntity<SupplierSummary[]> pending = rest.exchange(
                baseUrl("/api/admin/suppliers?status=PENDING"), HttpMethod.GET,
                new HttpEntity<>(authHeaders(managerToken)), SupplierSummary[].class);
        Long supplierId = List.of(pending.getBody()).stream()
                .filter(s -> s.getUsername().equals(supplierUsername))
                .findFirst().orElseThrow().getId();

        UpdateSupplierStatusRequest statusRequest = new UpdateSupplierStatusRequest();
        statusRequest.setStatus("ACTIVE");
        rest.exchange(
                baseUrl("/api/admin/suppliers/" + supplierId + "/status"), HttpMethod.PATCH,
                new HttpEntity<>(statusRequest, authHeaders(managerToken)), SupplierSummary.class);
        return supplierId;
    }
}
