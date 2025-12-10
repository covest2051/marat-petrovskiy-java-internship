package paymentservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record PaymentRequest(
        @NotNull
        Long orderId,

        @NotNull
        Long userId,

        @Size(max = 20)
        String status,

        BigDecimal paymentAmount) {
}
