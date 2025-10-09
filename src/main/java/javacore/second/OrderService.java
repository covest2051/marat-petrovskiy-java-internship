package javacore.second;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
public class OrderService {
    private List<Order> orders;

    public List<String> getUniqueOrdersCities(List<Order> orders) {
        List<String> uniqueOrdersCities = orders.stream()
                .map(order -> order.getCustomer().getCity())
                .distinct()
                .toList();

        return uniqueOrdersCities;
    }

    public Double getTotalOrdersIncome(List<Order> orders) {
        Double totalOrdersIncome = orders.stream()
                .filter(order -> order.getStatus().equals(OrderStatus.DELIVERED))
                .mapToDouble(order -> order.getItems().stream()
                        .mapToDouble(item -> item.getPrice() * item.getQuantity())
                        .sum())
                .sum();

        return totalOrdersIncome;
    }

    // Не совсем понял формулировку "The most popular product by sales". Можно трактовать
    // 1) как продукт, на который пользователи потратили больше всего денег, так и
    // 2) как продукт, который купили большее количество раз
    // Этот метод реализует логику 1), следующий метод реализует логику 2)
    public List<OrderItem> getMostPopularProductsBySales(List<Order> orders) {
        Map<String, Double> salesByProduct = orders.stream()
                .flatMap(order -> order.getItems().stream())
                .collect(Collectors.groupingBy(
                        OrderItem::getProductName,
                        Collectors.summingDouble(item -> item.getQuantity() * item.getPrice())
                ));

        double maxSales = salesByProduct.values().stream()
                .mapToDouble(Double::doubleValue)
                .max()
                .orElse(0.0);

        List<OrderItem> mostPopularProductsBySales = orders.stream()
                .flatMap(order -> order.getItems().stream())
                .filter(item -> {
                    double totalSales = salesByProduct.get(item.getProductName());
                    return totalSales == maxSales;
                })
                .distinct()
                .collect(Collectors.toList());

        return mostPopularProductsBySales;
    }

    public List<OrderItem> getMostPopularProductsByQuantity(List<Order> orders) {
        Map<String, Integer> quantityByProduct = orders.stream()
                .flatMap(order -> order.getItems().stream())
                .collect(Collectors.groupingBy(
                        OrderItem::getProductName,
                        Collectors.summingInt(OrderItem::getQuantity)
                ));

        int maxQuantity = quantityByProduct.values().stream()
                .mapToInt(Integer::intValue)
                .max()
                .orElse(0);

        List<OrderItem> mostPopularProductsByQuantity = orders.stream()
                .flatMap(order -> order.getItems().stream())
                .filter(item -> quantityByProduct.get(item.getProductName()) == maxQuantity)
                .distinct()
                .collect(Collectors.toList());

        return mostPopularProductsByQuantity;
    }

    public Double getAverageCheckForOrderWithStatusDelivered(List<Order> orders) {
        Double averageCheckForOrderWithStatusDelivered = orders.stream()
                .filter(order -> order.getStatus().equals(OrderStatus.DELIVERED))
                .mapToDouble(order -> order.getItems().stream()
                        .mapToDouble(item -> item.getPrice() * item.getQuantity())
                        .sum())
                .average()
                .orElse(0.0);

        return averageCheckForOrderWithStatusDelivered;
    }

    // Подумал, что лучше количество товаров для поиска передавать в параметрах
    // Если додумывать самому не стоит - исправлю и больше не буду :)
    public List<Customer> getCustomerWithOrdersMoreThanN(List<Order> orders, int n) {
        List<Customer> customerWithOrdersMoreThanN = orders.stream()
                .collect(Collectors.groupingBy(Order::getCustomer, Collectors.counting()))
                .entrySet()
                .stream()
                .filter(entry -> entry.getValue() > n)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        return customerWithOrdersMoreThanN;
    }

    public List<Order> initList() {
        orders = new ArrayList<>();

        orders.add(new Order(
                "O1",
                LocalDateTime.now().minusDays(10),
                new Customer("C1", "Alice", "alice@example.com", LocalDateTime.now().minusYears(2), 28, "Berlin"),
                List.of(
                        new OrderItem("Laptop", 1, 1200.0, Category.ELECTRONICS),
                        new OrderItem("Headphones", 2, 150.0, Category.ELECTRONICS)
                ),
                OrderStatus.DELIVERED
        ));

        orders.add(new Order(
                "O2",
                LocalDateTime.now().minusDays(7),
                new Customer("C2", "Bob", "bob@example.com", LocalDateTime.now().minusMonths(5), 34, "Munich"),
                List.of(
                        new OrderItem("T-shirt", 3, 25.0, Category.CLOTHING),
                        new OrderItem("Sneakers", 1, 80.0, Category.CLOTHING)
                ),
                OrderStatus.SHIPPED
        ));

        orders.add(new Order(
                "O3",
                LocalDateTime.now().minusDays(5),
                new Customer("C3", "Charlie", "charlie@example.com", LocalDateTime.now().minusMonths(8), 22, "Hamburg"),
                List.of(
                        new OrderItem("Book", 2, 18.0, Category.BOOKS),
                        new OrderItem("Lamp", 1, 45.0, Category.HOME)
                ),
                OrderStatus.PROCESSING
        ));

        orders.add(new Order(
                "O4",
                LocalDateTime.now().minusDays(3),
                new Customer("C4", "Diana", "diana@example.com", LocalDateTime.now().minusYears(1), 31, "Berlin"),
                List.of(
                        new OrderItem("Perfume", 1, 60.0, Category.BEAUTY),
                        new OrderItem("Hair Dryer", 1, 35.0, Category.HOME)
                ),
                OrderStatus.NEW
        ));

        orders.add(new Order(
                "O5",
                LocalDateTime.now().minusDays(2),
                new Customer("C5", "Ethan", "ethan@example.com", LocalDateTime.now().minusMonths(3), 40, "Cologne"),
                List.of(
                        new OrderItem("Toy Car", 2, 25.0, Category.TOYS),
                        new OrderItem("Puzzle", 1, 15.0, Category.TOYS)
                ),
                OrderStatus.CANCELLED
        ));

        orders.add(new Order(
                "O6",
                LocalDateTime.now().minusDays(1),
                new Customer("C1", "Alice", "alice@example.com", LocalDateTime.now().minusYears(2), 28, "Berlin"),
                List.of(
                        new OrderItem("E-book Reader", 1, 90.0, Category.ELECTRONICS),
                        new OrderItem("Notebook", 4, 5.0, Category.BOOKS)
                ),
                OrderStatus.DELIVERED
        ));

        return orders;
    }
}
