package orderservice.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import orderservice.entity.OrderItem;
import orderservice.entity.OrderStatus;

import java.util.List;

public record OrderRequest(
        @NotNull
        Long userId,

        @Size(max = 20)
        OrderStatus status,

        @NotEmpty
        List<OrderItem> orderItems) {
}
