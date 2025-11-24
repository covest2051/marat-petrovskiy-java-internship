package paymentservice.dto;

import paymentservice.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentResponse(
        Long id,

        Long orderId,

        Long userId,

        PaymentStatus status,

        Instant timestamp,

        BigDecimal paymentAmount) {
}
