package paymentservice.service;

import paymentservice.dto.PaymentRequest;
import paymentservice.dto.PaymentResponse;
import paymentservice.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public interface PaymentService {
    PaymentResponse createPayment(PaymentRequest paymentRequest);

    List<PaymentResponse> getPaymentsByOrderId(int page, int size, Long orderId);

    List<PaymentResponse> getPaymentsByUserId(int page, int size, Long userId);

    List<PaymentResponse> getPaymentsByStatus(int page, int size, PaymentStatus status);

    /**
     * Задача была описана как: Get total sum of payments for date period
     * Не уточняется, сумма заказов всех пользователей или одного конкретного
     * Поэтому реализовал два варианта
     * Но лично на мой взгляд, сумма заказов всех пользователей нужна больше для ведения статистики какой-нибудь
     * Поэтому я бы реализовывал её за пределами CRUD сервиса.
     */
    BigDecimal getAllPaymentsByPeriod(int page, int size, Instant from, Instant to);

    BigDecimal getUserPaymentsByPeriod(int page, int size, Long userId, Instant from, Instant to);
}
