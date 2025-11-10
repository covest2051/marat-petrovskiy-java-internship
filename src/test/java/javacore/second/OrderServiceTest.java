package javacore.second;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderServiceTest {

    private OrderService orderService;
    private List<Order> orders;
    @BeforeEach
    void setUp() {
        orderService = new OrderService();
        orders = orderService.initList();
    }

    @Test
    void testGetUniqueOrdersCities() {
        List<String> uniqueOrdersCities = orderService.getUniqueOrdersCities(orders);

        assertNotNull(uniqueOrdersCities);
        assertEquals(4, uniqueOrdersCities.size());
        assertIterableEquals(
                List.of("Berlin", "Munich", "Hamburg", "Cologne"),
                uniqueOrdersCities
        );
    }

    @Test
    void testGetUniqueOrdersCitiesOnEmptyList() {
        orders.clear();
        List<String> uniqueOrdersCities = orderService.getUniqueOrdersCities(orders);

        assertNotNull(uniqueOrdersCities);
        assertEquals(0, uniqueOrdersCities.size());
    }

    @Test
    void testGetTotalOrdersIncome() {
        Double totalIncome = orderService.getTotalOrdersIncome(orders);

        assertEquals(1610.0, totalIncome);
    }

    @Test
    void testGetTotalOrdersIncomeOnSingletonList() {
        orders.clear();

        Customer marat = new Customer("C6", "Marat", "maratpetrovitch@gmail.com", LocalDateTime.of(2024, 5, 15, 10, 0), 34, "Minsk");

        orders.add(new Order(
                "O1",
                LocalDateTime.now().minusHours(48),
                marat,
                List.of(
                        new OrderItem("Jeans", 1, 70.0, Category.CLOTHING)
                ),
                OrderStatus.DELIVERED
        ));

        Double totalIncome = orderService.getTotalOrdersIncome(orders);

        assertEquals(70, totalIncome);
    }

    @Test
    void testGetMostPopularProductsBySales() {
        List<OrderItem> mostPopularProductsBySales = orderService.getMostPopularProductsBySales(orders);

        assertEquals("Laptop", mostPopularProductsBySales.getFirst().getProductName());
    }

    @Test
    void testGetMostPopularProductsByQuantity() {
        List<OrderItem> mostPopularProductsByQuantity = orderService.getMostPopularProductsByQuantity(orders);

        assertEquals("Notebook", mostPopularProductsByQuantity.getFirst().getProductName());
    }

    @Test
    void testGetMostPopularProductsByQuantityWithProductsWithSameQuantity() {
        orders.clear();

        Customer marat = new Customer("C6", "Marat", "maratpetrovitch@gmail.com", LocalDateTime.of(2024, 5, 15, 10, 0), 34, "Minsk");

        Order O1 = new Order(
                "O1",
                LocalDateTime.now().minusHours(48),
                marat,
                List.of(
                        new OrderItem("Jeans", 1, 70.0, Category.CLOTHING)
                ),
                OrderStatus.DELIVERED
        );

        Order O2 =new Order(
                "O2",
                LocalDateTime.now().minusHours(45),
                marat,
                List.of(
                        new OrderItem("Hoodie", 1, 70.0, Category.CLOTHING)
                ),
                OrderStatus.DELIVERED
        );

        orders.add(O1);
        orders.add(O2);

        List<OrderItem> mostPopularProductsBySales = orderService.getMostPopularProductsBySales(orders);

        assertEquals(2, mostPopularProductsBySales.size());

        List<String> productNames = mostPopularProductsBySales.stream().map(OrderItem::getProductName).toList();
        assertTrue(productNames.contains("Jeans"));
        assertTrue(productNames.contains("Hoodie"));
    }

    @Test
    void testGetAverageCheckForOrderWithStatusDelivered() {
        Double averageCheckForOrderWithStatusDelivered = orderService.getAverageCheckForOrderWithStatusDelivered(orders);

        assertEquals(805.0, averageCheckForOrderWithStatusDelivered);
    }

    @Test
    void testGetCustomerWithOrdersMoreThanN() {
        Customer marat = new Customer("C6", "Marat", "maratpetrovitch@gmail.com", LocalDateTime.of(2024, 5, 15, 10, 0), 34, "Minsk");

        orders.add(new Order(
                "O7",
                LocalDateTime.now().minusHours(48),
                marat,
                List.of(
                        new OrderItem("Jeans", 1, 70.0, Category.CLOTHING)
                ),
                OrderStatus.DELIVERED
        ));

        orders.add(new Order(
                "O8",
                LocalDateTime.now().minusHours(36),
                marat,
                List.of(
                        new OrderItem("Coffee Maker", 1, 120.0, Category.HOME)
                ),
                OrderStatus.SHIPPED
        ));

        orders.add(new Order(
                "O9",
                LocalDateTime.now().minusHours(24),
                marat,
                List.of(
                        new OrderItem("Smart Watch", 1, 200.0, Category.ELECTRONICS)
                ),
                OrderStatus.PROCESSING
        ));

        orders.add(new Order(
                "O10",
                LocalDateTime.now().minusHours(12),
                marat,
                List.of(
                        new OrderItem("Backpack", 1, 45.0, Category.CLOTHING)
                ),
                OrderStatus.NEW
        ));

        orders.add(new Order(
                "O11",
                LocalDateTime.now().minusHours(6),
                marat,
                List.of(
                        new OrderItem("Bluetooth Speaker", 1, 60.0, Category.ELECTRONICS)
                ),
                OrderStatus.NEW
        ));

        orders.add(new Order(
                "O12",
                LocalDateTime.now().minusHours(3),
                marat,
                List.of(
                        new OrderItem("Bluetooth Speaker", 1, 60.0, Category.ELECTRONICS)
                ),
                OrderStatus.NEW
        ));

        List<Customer> customerWithFiveMoreOrders = orderService.getCustomerWithOrdersMoreThanN(orders, 5);

        assertEquals(1, customerWithFiveMoreOrders.size());
    }

    @Test
    void testGetCustomerWithOrdersMoreThanNWithResultZero() {
        List<Customer> customerWithFiveMoreOrders = orderService.getCustomerWithOrdersMoreThanN(orders, 5);

        assertEquals(0, customerWithFiveMoreOrders.size());
    }
}
