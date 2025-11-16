package paymentservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import paymentservice.entity.PaymentStatus;

import java.time.Instant;

public record PaymentResponse(
        Long id,

        Long orderId,

        Long userId,

        PaymentStatus status,

        Instant timestamp,

        Long paymentAmount) {
}
