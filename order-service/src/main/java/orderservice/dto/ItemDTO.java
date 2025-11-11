package orderservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record ItemDTO(
        Long id,

        @NotBlank
        String name,

        @Positive
        BigDecimal price
) {}
