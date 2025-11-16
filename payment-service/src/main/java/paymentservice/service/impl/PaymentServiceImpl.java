package paymentservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import paymentservice.dto.PaymentRequest;
import paymentservice.dto.PaymentResponse;
import paymentservice.dto.mapper.PaymentMapper;
import paymentservice.entity.Payment;
import paymentservice.entity.PaymentStatus;
import paymentservice.repository.PaymentRepository;
import paymentservice.service.PaymentService;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentMapper paymentMapper;
    private final PaymentRepository paymentRepository;

    @Override
    @Transactional
    public PaymentResponse createPayment(PaymentRequest paymentRequest) {
        Payment payment = Payment.builder()
                .orderId(paymentRequest.orderId())
                .userId(paymentRequest.userId())
                .status(PaymentStatus.valueOf(paymentRequest.status()))
                .timestamp(Instant.now())
                .paymentAmount(paymentRequest.paymentAmount())
                .build();

        Payment savedPayment = paymentRepository.save(payment);

        return paymentMapper.toPaymentResponse(savedPayment);
    }

    @Override
    public List<PaymentResponse> getPaymentsByOrderId(int page, int size, Long orderId) {
        Pageable pageable = PageRequest.of(page, size);
        List<Payment> payments = paymentRepository.findByOrderId(orderId, pageable);

        return paymentMapper.toPaymentResponseList(payments);
    }

    @Override
    public List<PaymentResponse> getPaymentsByUserId(int page, int size, Long userId) {
        Pageable pageable = PageRequest.of(page, size);
        List<Payment> payments = paymentRepository.findAllByUserId(userId, pageable);

        return paymentMapper.toPaymentResponseList(payments);
    }

    @Override
    public List<PaymentResponse> getPaymentsByStatus(int page, int size, PaymentStatus status) {
        Pageable pageable = PageRequest.of(page, size);
        List<Payment> payments = paymentRepository.findAllByStatus(status, pageable);

        return paymentMapper.toPaymentResponseList(payments);
    }

    @Override
    public BigDecimal getAllPaymentsByPeriod(int page, int size, Instant from, Instant to) {
        Pageable pageable = PageRequest.of(page, size);

        return paymentRepository.getTotalSumForPeriod(from, to, pageable);
    }

    @Override
    public BigDecimal getUserPaymentsByPeriod(int page, int size, Long userId, Instant from, Instant to) {
        Pageable pageable = PageRequest.of(page, size);

        return paymentRepository.getTotalSumForPeriodByUser(userId, from, to, pageable);
    }
}
