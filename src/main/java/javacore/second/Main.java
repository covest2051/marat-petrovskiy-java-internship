package javacore.second;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        OrderService orderService = new OrderService();
        List<Order> orders = orderService.initList();
        orderService.getUniqueOrdersCities(orders);
    }
}
