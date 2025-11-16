package paymentservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import paymentservice.entity.PaymentStatus;

import java.time.Instant;

public record PaymentResponse(
        Long id,

        @NotNull
        Long orderId,

        @NotNull
        Long userId,

        @Size(max = 20)
        PaymentStatus status,

        Instant timestamp,

        Long paymentAmount) {
}
