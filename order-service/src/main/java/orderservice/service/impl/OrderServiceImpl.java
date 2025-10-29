package orderservice.service.impl;

import lombok.RequiredArgsConstructor;
import orderservice.dto.OrderRequest;
import orderservice.dto.OrderResponse;
import orderservice.dto.mapper.OrderMapper;
import orderservice.entity.Order;
import orderservice.entity.OrderStatus;
import orderservice.exception.OrderNotFoundException;
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

    @Override
    @Transactional
    public OrderResponse createOrder(OrderRequest orderRequest) {
        Order order = Order.builder()
                .userId(orderRequest.userId())
                .status(OrderStatus.CREATED)
                .creationDate(LocalDateTime.now())
                .orderItems(orderRequest.orderItems())
                .build();

        Order savedOrder = orderRepository.save(order);

        return orderMapper.toOrderResponse(savedOrder);
    }

    @Override
    @Cacheable(value = "orders", key = "#id")
    public OrderResponse getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException("Order with id " + id + " not found"));

        return orderMapper.toOrderResponse(order);
    }

    @Override
    public List<OrderResponse> getAllUserOrdersById(int page, int size, Long userId) {
        Pageable pageable = PageRequest.of(page, size);
        List<Order> orders = orderRepository.findAllByUserId(userId, pageable);

        return orderMapper.toOrderResponseList(orders);
    }

    @Override
    public List<OrderResponse> getAllOrdersByStatus(int page, int size, OrderStatus status) {
        Pageable pageable = PageRequest.of(page, size);
        List<Order> orders = orderRepository.findAllByStatus(status, pageable);

        return orderMapper.toOrderResponseList(orders);
    }

    @Override
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

        return orderMapper.toOrderResponse(savedOrder);
    }

    @Override
    public void deleteOrder(Long id) {
        Order orderToDelete = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException("Order with id " + id + " not found"));

        orderRepository.delete(orderToDelete);
    }
}
