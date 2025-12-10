import orderservice.client.UserClient;
import orderservice.dto.OrderRequest;
import orderservice.dto.OrderResponse;
import orderservice.dto.UserResponse;
import orderservice.dto.mapper.OrderMapper;
import orderservice.entity.Order;
import orderservice.entity.OrderStatus;
import orderservice.exception.OrderNotFoundException;
import orderservice.repository.OrderRepository;
import orderservice.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserClient userClient;

    @Mock
    private OrderMapper orderMapper;

    @InjectMocks
    private OrderServiceImpl orderService;

    private Order order;
    private OrderRequest orderRequest;
    private UserResponse userResponse;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        orderRequest = new OrderRequest(1L, OrderStatus.CREATED, List.of());

        order = Order.builder()
                .id(1L)
                .userId(1L)
                .status(OrderStatus.CREATED)
                .creationDate(LocalDateTime.now())
                .orderItems(List.of())
                .build();

        userResponse = new UserResponse(1L, "Marat", "Petrovskiy", LocalDate.of(2000, 1, 1), "maratpetrovitch@gmail.com");
    }

    @Test
    void createOrder_shouldReturnOrderResponseWithUserInfo() {
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(userClient.getUserById(eq(1L))).thenReturn(userResponse);
        when(orderMapper.toOrderResponse(eq(order), eq(userResponse)))
                .thenReturn(new OrderResponse(order.getId(), userResponse, order.getStatus(), order.getCreationDate(), order.getOrderItems()));

        OrderResponse response = orderService.createOrder(orderRequest);

        assertNotNull(response);
        assertEquals(1L, response.userResponse().id());
        verify(orderRepository, times(1)).save(any(Order.class));
        verify(userClient, times(1)).getUserById(eq(1L));
        verify(orderMapper, times(1)).toOrderResponse(eq(order), eq(userResponse));
    }

    @Test
    void getAllUserOrdersById_shouldReturnOrdersWithUserInfo() {
        when(orderRepository.findAllByUserId(anyLong(), any(Pageable.class))).thenReturn(List.of(order));
        when(userClient.getUserById(eq(1L))).thenReturn(userResponse);
        when(orderMapper.toOrderResponseList(anyList()))
                .thenReturn(List.of(new OrderResponse(order.getId(), userResponse, order.getStatus(), order.getCreationDate(), order.getOrderItems())));

        List<OrderResponse> orders = orderService.getAllUserOrdersById(0, 10, 1L);

        assertEquals(1, orders.size());
        verify(orderRepository, times(1)).findAllByUserId(eq(1L), any(Pageable.class));
        verify(userClient, times(1)).getUserById(eq(1L));
    }

    @Test
    void getOrderById_shouldReturnOrderWithUserInfo() {
        when(orderRepository.findById(eq(1L))).thenReturn(Optional.of(order));
        when(userClient.getUserById(eq(1L))).thenReturn(userResponse);
        when(orderMapper.toOrderResponse(eq(order), eq(userResponse)))
                .thenReturn(new OrderResponse(order.getId(), userResponse, order.getStatus(), order.getCreationDate(), order.getOrderItems()));

        OrderResponse response = orderService.getOrderById(1L);

        assertNotNull(response);
        assertEquals(1L, response.userResponse().id());
        verify(orderRepository, times(1)).findById(eq(1L));
        verify(userClient, times(1)).getUserById(eq(1L));
    }

    @Test
    void getOrderById_shouldThrowOrderNotFoundException() {
        when(orderRepository.findById(eq(2L))).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> orderService.getOrderById(2L));
        verify(orderRepository, times(1)).findById(eq(2L));
        verify(userClient, never()).getUserById(anyLong());
    }

    @Test
    void getAllOrdersByStatus_shouldReturnListWithUser() {
        when(orderRepository.findAllByStatus(any(OrderStatus.class), any(Pageable.class))).thenReturn(List.of(order));
        when(userClient.getUserById(eq(1L))).thenReturn(userResponse);
        when(orderMapper.toOrderResponseList(eq(List.of(order))))
                .thenReturn(List.of(new OrderResponse(order.getId(), userResponse, order.getStatus(), order.getCreationDate(), order.getOrderItems())));

        List<OrderResponse> result = orderService.getAllOrdersByStatus(0, 10, OrderStatus.CREATED);

        assertEquals(1, result.size());
        assertEquals("Marat", result.get(0).userResponse().name());
        verify(orderRepository, times(1)).findAllByStatus(eq(OrderStatus.CREATED), any(Pageable.class));
    }

    @Test
    void updateOrder_shouldReturnUpdated() {
        OrderRequest request = new OrderRequest(1L, OrderStatus.PROCESSING, List.of());

        Order updatedOrder = Order.builder()
                .id(1L)
                .userId(1L)
                .status(OrderStatus.CREATED)
                .creationDate(order.getCreationDate())
                .orderItems(List.of())
                .build();

        when(orderRepository.findById(eq(1L))).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(updatedOrder);
        when(userClient.getUserById(eq(1L))).thenReturn(userResponse);
        when(orderMapper.toOrderResponse(eq(updatedOrder), eq(userResponse)))
                .thenReturn(new OrderResponse(updatedOrder.getId(), userResponse, updatedOrder.getStatus(), updatedOrder.getCreationDate(), updatedOrder.getOrderItems()));

        OrderResponse resp = orderService.updateOrder(1L, request);

        assertNotNull(resp);
        assertEquals(OrderStatus.CREATED, resp.status());
        verify(orderRepository, times(1)).findById(eq(1L));
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    void updateOrder_shouldThrowIllegalStateException_whenOrderStatusIsPayed() {
        Order existingOrder = Order.builder()
                .id(1L)
                .userId(1L)
                .status(OrderStatus.PAYED)
                .creationDate(order.getCreationDate())
                .orderItems(List.of())
                .build();

        OrderRequest request = new OrderRequest(1L, OrderStatus.CREATED, List.of());

        when(orderRepository.findById(eq(1L))).thenReturn(Optional.of(existingOrder));

        assertThrows(IllegalStateException.class, () -> orderService.updateOrder(1L, request));
        verify(orderRepository, never()).save(any());
        verify(orderMapper, never()).toOrderResponse(any(), any());
    }

    @Test
    void deleteOrder_shouldDeleteExisting() {
        when(orderRepository.findById(eq(1L))).thenReturn(Optional.of(order));
        orderService.deleteOrder(1L);
        verify(orderRepository, times(1)).delete(eq(order));
    }

    @Test
    void deleteOrder_shouldThrow_whenNotFound() {
        when(orderRepository.findById(eq(5L))).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> orderService.deleteOrder(5L));
        verify(orderRepository, never()).delete(any());
    }
}
