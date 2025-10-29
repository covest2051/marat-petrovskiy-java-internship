package orderservice.dto;

import jakarta.validation.constraints.Size;
import orderservice.entity.OrderItem;
import orderservice.entity.OrderStatus;

import java.util.List;

public record OrderRequest(
        Long userId,

        @Size(max = 20)
        OrderStatus status,

        List<OrderItem> orderItems) {
}
