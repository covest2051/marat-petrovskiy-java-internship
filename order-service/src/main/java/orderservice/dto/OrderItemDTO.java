package orderservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record OrderItemDTO(
        Long id,

        @NotNull
        ItemDTO item,

        @Positive
        Integer quantity
) {}
