package orderservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Size;
import orderservice.entity.OrderItem;
import orderservice.entity.OrderStatus;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderResponse(
        Long id,

        UserResponse userResponse,

        @Size(max = 20)
        OrderStatus status,

        LocalDateTime creationDate,

        List<OrderItemDTO>orderItems) implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
}
