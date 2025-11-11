package orderservice.service;

import orderservice.dto.OrderRequest;
import orderservice.dto.OrderResponse;
import orderservice.entity.OrderStatus;

import java.util.List;

public interface OrderService {
    OrderResponse createOrder(OrderRequest orderRequest);

    OrderResponse getOrderById(Long id);

    List<OrderResponse> getAllUserOrdersById(int page, int size, Long userId);

    List<OrderResponse> getAllOrdersByStatus(int page, int size, OrderStatus status);

    OrderResponse updateOrder(Long id, OrderRequest orderRequest);

    void deleteOrder(Long id);
}
