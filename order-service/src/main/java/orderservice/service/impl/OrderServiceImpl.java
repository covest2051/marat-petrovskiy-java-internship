package orderservice.service.impl;

import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import orderservice.client.UserClient;
import orderservice.dto.OrderRequest;
import orderservice.dto.OrderResponse;
import orderservice.dto.UserResponse;
import orderservice.dto.mapper.OrderMapper;
import orderservice.entity.Order;
import orderservice.entity.OrderStatus;
import orderservice.exception.OrderNotFoundException;
import orderservice.metrics.OrderMetrics;
import orderservice.repository.OrderRepository;
import orderservice.service.OrderService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final UserClient userClient;
    private final OrderMetrics orderMetrics;

    @Override
    @Transactional
    @Timed(value = "order.create.time", description = "Time spent creating orders")
    public OrderResponse createOrder(OrderRequest orderRequest) {
        Order order = Order.builder()
                .userId(orderRequest.userId())
                .status(OrderStatus.CREATED)
                .creationDate(LocalDateTime.now())
                .orderItems(orderRequest.orderItems())
                .build();

        UserResponse user = userClient.getUserById(order.getUserId());

        Order savedOrder = orderRepository.save(order);

        orderMetrics.incrementCreated();

        return orderMapper.toOrderResponse(savedOrder, user);
    }

    @Override
    @Cacheable(value = "orders", key = "#id")
    public OrderResponse getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException("Order with id " + id + " not found"));

        UserResponse user = userClient.getUserById(order.getUserId());

        return orderMapper.toOrderResponse(order, user);
    }

    @Override
    public List<OrderResponse> getAllUserOrdersById(int page, int size, Long userId) {
        Pageable pageable = PageRequest.of(page, size);
        List<Order> orders = orderRepository.findAllByUserId(userId, pageable);

        UserResponse user = userClient.getUserById(userId);

        return orders.stream()
                .map(order -> orderMapper.toOrderResponse(order, user))
                .toList();
    }

    @Override
    public List<OrderResponse> getAllOrdersByStatus(int page, int size, OrderStatus status) {
        Pageable pageable = PageRequest.of(page, size);
        List<Order> orders = orderRepository.findAllByStatus(status, pageable);

        return orderMapper.toOrderResponseList(orders);
    }

    @Override
    @Transactional
    public OrderResponse updateOrder(Long id, OrderRequest orderRequest) {
        Order orderToUpdate = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException("Order with id " + id + " not found"));

        if (orderRequest.status().ordinal() >= OrderStatus.PAYED.ordinal()) {
            throw new IllegalStateException("You cannot edit order after it has been payed");
        }

        if (orderToUpdate.getStatus().ordinal() >= orderRequest.status().ordinal()) {
            throw new IllegalStateException("It`s not allowed to change status in opposite direction");
        }

        orderToUpdate.setStatus(orderRequest.status());
        orderToUpdate.setOrderItems(orderRequest.orderItems());

        Order savedOrder = orderRepository.save(orderToUpdate);

        UserResponse user = userClient.getUserById(orderRequest.userId());

        return orderMapper.toOrderResponse(savedOrder, user);
    }

    @Override
    @Transactional
    public void deleteOrder(Long id) {
        Order orderToDelete = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException("Order with id " + id + " not found"));

        orderRepository.delete(orderToDelete);
    }
}
