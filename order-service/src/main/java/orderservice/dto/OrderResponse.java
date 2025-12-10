package orderservice.dto;

import jakarta.validation.constraints.Size;
import orderservice.entity.OrderItem;
import orderservice.entity.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id,

        UserResponse userResponse,

        @Size(max = 20)
        OrderStatus status,

        LocalDateTime creationDate,

        List<OrderItem>orderItems,

        UserResponse user) {
}
