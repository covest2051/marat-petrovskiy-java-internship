package integration;

import com.github.tomakehurst.wiremock.WireMockServer;
import orderservice.OrderServiceApplication;
import orderservice.dto.OrderRequest;
import orderservice.dto.OrderResponse;
import orderservice.entity.Order;
import orderservice.entity.OrderStatus;
import orderservice.exception.OrderNotFoundException;
import orderservice.repository.OrderRepository;
import orderservice.service.OrderService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(classes = OrderServiceApplication.class,
        properties = {
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration"
        })
class OrderServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("innowisedb")
            .withUsername("postgres")
            .withPassword("postgres");

    @DynamicPropertySource
    static void registerPgProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    private WireMockServer wireMockServer;

    @BeforeEach
    void setupWireMock() {
        wireMockServer = new WireMockServer(8089);
        wireMockServer.start();
        configureFor("localhost", 8089);

        stubFor(get(urlEqualTo("/users/id/1"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "id": 1,
                                  "name": "Marat",
                                  "surname": "Petrovskiy",
                                  "birthDate": "2000-01-01",
                                  "email": "maratpetrovitch@gmail.com"
                                }
                                """)));
    }

    @AfterEach
    void tearDown() {
        if (wireMockServer != null && wireMockServer.isRunning()) {
            wireMockServer.stop();
        }

        orderRepository.deleteAll();
    }

    @Test
    void createOrder_shouldSaveOrder() {
        OrderRequest orderRequest = new OrderRequest(1L, OrderStatus.CREATED, List.of());
        OrderResponse orderResponse = orderService.createOrder(orderRequest);

        assertThat(orderResponse).isNotNull();
        assertThat(orderResponse.userResponse().name()).isEqualTo("Marat");
        assertThat(orderRepository.findById(orderResponse.id())).isPresent();
    }

    @Test
    void getOrderById_shouldReturnExistingOrder() {
        OrderRequest orderRequest = new OrderRequest(1L, OrderStatus.CREATED, List.of());
        OrderResponse orderResponse = orderService.createOrder(orderRequest);

        OrderResponse orderFromDatabase = orderService.getOrderById(orderResponse.id());

        assertThat(orderFromDatabase).isNotNull();
        assertThat(orderFromDatabase.id()).isEqualTo(orderResponse.id());
        assertThat(orderFromDatabase.userResponse().email()).isEqualTo("maratpetrovitch@gmail.com");
    }

    @Test
    void getOrderById_shouldThrowOrderNotFoundException() {
        assertThat(org.junit.jupiter.api.Assertions.assertThrows(
                OrderNotFoundException.class, () -> orderService.getOrderById(999L)
        )).isInstanceOf(RuntimeException.class);
    }

    @Test
    void getAllOrdersByStatus_shouldReturnOrderResponseList() {
        orderService.createOrder(new OrderRequest(1L, OrderStatus.CREATED, List.of()));
        orderService.createOrder(new OrderRequest(1L, OrderStatus.CREATED, List.of()));

        List<OrderResponse> orders = orderService.getAllOrdersByStatus(0, 10, OrderStatus.CREATED);

        assertThat(orders).hasSize(2);
        assertThat(orders.get(0).userResponse().surname()).isEqualTo("Petrovskiy");
    }

    @Test
    void updateOrder_shouldUpdateStatus() {
        OrderResponse createdOrder = orderService.createOrder(new OrderRequest(1L, OrderStatus.CREATED, List.of()));

        OrderRequest updatedOrderRequest = new OrderRequest(1L, OrderStatus.PROCESSING, List.of());
        OrderResponse updatedOrderResponse = orderService.updateOrder(createdOrder.id(), updatedOrderRequest);

        assertThat(updatedOrderResponse.status()).isEqualTo(OrderStatus.PROCESSING);
        Optional<Order> updatedInDb = orderRepository.findById(createdOrder.id());
        assertThat(updatedInDb).isPresent();
        assertThat(updatedInDb.get().getStatus()).isEqualTo(OrderStatus.PROCESSING);
    }

    @Test
    void updateOrder_shouldThrowIllegalStateException() {
        Order order = orderRepository.save(Order.builder()
                .userId(1L)
                .status(OrderStatus.PAYED)
                .orderItems(List.of())
                .build());

        OrderRequest updatedOrder = new OrderRequest(1L, OrderStatus.CREATED, List.of());

        assertThat(org.junit.jupiter.api.Assertions.assertThrows(
                IllegalStateException.class, () -> orderService.updateOrder(order.getId(), updatedOrder)
        )).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void deleteOrder_shouldRemoveOrderFromDatabase() {
        OrderResponse createdOrder = orderService.createOrder(new OrderRequest(1L, OrderStatus.CREATED, List.of()));

        orderService.deleteOrder(createdOrder.id());

        assertThat(orderRepository.findById(createdOrder.id())).isEmpty();
    }

    @Test
    void deleteOrder_shouldThrowOrderNotFoundException() {
        assertThat(Assertions.assertThrows(
                OrderNotFoundException.class, () -> orderService.deleteOrder(10L)
        )).isInstanceOf(RuntimeException.class);
    }
}
