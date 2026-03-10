package orderservice.service.impl;

import io.micrometer.core.annotation.Timed;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import orderservice.client.UserClient;
import orderservice.dto.OrderRequest;
import orderservice.dto.OrderResponse;
import orderservice.dto.UserResponse;
import orderservice.dto.mapper.OrderEventMapper;
import orderservice.dto.mapper.OrderItemMapper;
import orderservice.dto.mapper.OrderMapper;
import orderservice.entity.Order;
import orderservice.entity.OrderItem;
import orderservice.entity.OrderStatus;
import orderservice.exception.OrderNotFoundException;
import orderservice.kafka.OrderEventProducer;
import orderservice.metrics.OrderMetrics;
import orderservice.repository.OrderRepository;
import orderservice.service.OrderService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final UserClient userClient;
    private final OrderMetrics orderMetrics;
    private final OrderItemMapper orderItemMapper;
    private final OrderEventProducer orderEventProducer;

    @Override
    @Transactional
    @Timed(value = "orderservice.orders.create.duration", description = "Время создания заказа")
    @Observed(name = "orderservice.orders.create", contextualName = "create-order")
    public OrderResponse createOrder(OrderRequest orderRequest) {
        log.info("Создание заказа для userId={}", orderRequest.userId());

        Order order = Order.builder()
                .userId(orderRequest.userId())
                .status(OrderStatus.CREATED)
                .creationDate(LocalDateTime.now())
                .orderItems(new ArrayList<>())
                .build();

        if (orderRequest.orderItems() != null) {
            orderRequest.orderItems().forEach(dto -> {
                OrderItem entity = orderItemMapper.toOrderItem(dto);
                order.addOrderItem(entity);
            });
        }

        UserResponse user = userClient.getUserById(order.getUserId());

        Order savedOrder = orderRepository.save(order);

        orderMetrics.incrementCreated();
        orderEventProducer.sendOrderCreatedEvent(savedOrder);

        log.info("Заказ создан: id={} userId={} статус={}", savedOrder.getId(), savedOrder.getUserId(), savedOrder.getStatus());
        return orderMapper.toOrderResponse(savedOrder, user);
    }

    @Override
    @Cacheable(value = "orders", key = "#id")
    @Observed(name = "orderservice.orders.get-by-id", contextualName = "get-order-by-id")
    public OrderResponse getOrderById(Long id) {
        log.debug("Запрос заказа id={}", id);

        Order order = orderRepository.findById(id).orElseThrow(() -> {
            orderMetrics.incrementNotFound();
            log.warn("Заказ не найден id={}", id);
            return new OrderNotFoundException("Order with id " + id + " not found");
        });

        UserResponse user = userClient.getUserById(order.getUserId());
        return orderMapper.toOrderResponse(order, user);
    }

    @Override
    @Observed(name = "orderservice.orders.get-by-user", contextualName = "get-user-orders")
    public List<OrderResponse> getAllUserOrdersById(int page, int size, Long userId) {
        log.debug("Запрос заказов userId={} page={} size={}", userId, page, size);

        Pageable pageable = PageRequest.of(page, size);
        List<Order> orders = orderRepository.findAllByUserId(userId, pageable);
        UserResponse user = userClient.getUserById(userId);

        return orders.stream()
                .map(order -> orderMapper.toOrderResponse(order, user))
                .toList();
    }

    @Override
    @Observed(name = "orderservice.orders.get-by-status", contextualName = "get-orders-by-status")
    public List<OrderResponse> getAllOrdersByStatus(int page, int size, OrderStatus status) {
        log.debug("Запрос заказов по статусу={} page={} size={}", status, page, size);

        Pageable pageable = PageRequest.of(page, size);
        List<Order> orders = orderRepository.findAllByStatus(status, pageable);
        return orderMapper.toOrderResponseList(orders);
    }

    @Override
    @Transactional
    @Observed(name = "orderservice.orders.update", contextualName = "update-order")
    public OrderResponse updateOrder(Long id, OrderRequest orderRequest) {
        log.info("Обновление заказа id={}", id);

        Order orderToUpdate = orderRepository.findById(id).orElseThrow(() -> {
            orderMetrics.incrementNotFound();
            return new OrderNotFoundException("Order with id " + id + " not found");
        });

        if (orderToUpdate.getStatus().ordinal() >= OrderStatus.PAYED.ordinal()) {
            orderMetrics.incrementIllegalStatusChange();
            log.warn("Попытка изменить оплаченный заказ id={}", id);
            throw new IllegalStateException("You cannot edit order after it has already been payed");
        }

        if (orderToUpdate.getStatus().ordinal() > orderRequest.status().ordinal()) {
            orderMetrics.incrementIllegalStatusChange();
            log.warn("Попытка понизить статус заказа id={} с {} на {}", id, orderToUpdate.getStatus(), orderRequest.status());
            throw new IllegalStateException("It's not allowed to change status in opposite direction");
        }

        orderToUpdate.setStatus(orderRequest.status());

        if (orderRequest.orderItems() != null) {
            orderToUpdate.getOrderItems().clear();
            orderRequest.orderItems().forEach(dto -> orderToUpdate.addOrderItem(orderItemMapper.toOrderItem(dto)));
        }

        Order savedOrder = orderRepository.save(orderToUpdate);
        UserResponse user = userClient.getUserById(orderRequest.userId());

        log.info("Заказ обновлён id={} новый статус={}", savedOrder.getId(), savedOrder.getStatus());
        return orderMapper.toOrderResponse(savedOrder, user);
    }

    @Override
    @Transactional
    public void deleteOrder(Long id) {
        log.info("Удаление заказа id={}", id);

        Order orderToDelete = orderRepository.findById(id).orElseThrow(() -> {
            orderMetrics.incrementNotFound();
            return new OrderNotFoundException("Order with id " + id + " not found");
        });

        orderRepository.delete(orderToDelete);
        orderMetrics.incrementDeleted();

        log.info("Заказ удалён id={}", id);
    }

    @Transactional
    public void updateOrderStatus(Long id, OrderStatus newStatus) {
        log.info("Обновление статуса заказа id={} -> {}", id, newStatus);

        Order order = orderRepository.findById(id).orElseThrow(() -> {
            orderMetrics.incrementNotFound();
            return new OrderNotFoundException("Order with id " + id + " not found");
        });

        if (order.getStatus().ordinal() >= OrderStatus.PAYED.ordinal()) {
            orderMetrics.incrementIllegalStatusChange();
            throw new IllegalStateException("You cannot edit order after it has already been payed");
        }

        if (order.getStatus().ordinal() > newStatus.ordinal()) {
            orderMetrics.incrementIllegalStatusChange();
            throw new IllegalStateException("It's not allowed to change status in opposite direction");
        }

        order.setStatus(newStatus);
        orderRepository.save(order);

        log.info("Статус заказа id={} изменён на {}", id, newStatus);
    }
}
