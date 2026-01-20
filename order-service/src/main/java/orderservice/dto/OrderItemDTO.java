package orderservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.io.Serial;
import java.io.Serializable;

public record OrderItemDTO(
        Long id,

        @NotNull
        ItemDTO item,

        @Positive
        Integer quantity)implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
}