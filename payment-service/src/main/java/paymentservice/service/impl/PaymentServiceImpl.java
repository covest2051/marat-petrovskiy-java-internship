package paymentservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import paymentservice.client.RandomNumberClient;
import paymentservice.dto.PaymentRequest;
import paymentservice.dto.PaymentResponse;
import paymentservice.dto.mapper.PaymentMapper;
import paymentservice.entity.Payment;
import paymentservice.entity.PaymentStatus;
import paymentservice.kafka.PaymentEventProducer;
import paymentservice.kafka.event.OrderCreatedEvent;
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
    private final RandomNumberClient randomNumberClient;
    private final PaymentEventProducer paymentEventProducer;

    @Override
    @Transactional
    public PaymentResponse createPayment(PaymentRequest paymentRequest) {
        int randomNumber = randomNumberClient.getRandomNumber();

        PaymentStatus status = (randomNumber % 2 == 0)
                ? PaymentStatus.CREATED
                : PaymentStatus.ERROR;

        Payment payment = Payment.builder()
                .orderId(paymentRequest.orderId())
                .userId(paymentRequest.userId())
                .status(status)
                .timestamp(Instant.now())
                .paymentAmount(paymentRequest.paymentAmount())
                .build();

        Payment savedPayment = paymentRepository.save(payment);

        paymentEventProducer.sendPaymentCreatedEvent(payment);

        return paymentMapper.toPaymentResponse(savedPayment);
    }

    @Override
    @Transactional
    public PaymentResponse createPaymentFromOrder(OrderCreatedEvent event) {
        PaymentRequest paymentRequest = new PaymentRequest(
                event.orderId(),
                event.userId(),
                "PENDING",
                event.paymentAmount()
        );

        int randomNumber = randomNumberClient.getRandomNumber();

        PaymentStatus status = (randomNumber % 2 == 0)
                ? PaymentStatus.CREATED
                : PaymentStatus.ERROR;

        Payment payment = Payment.builder()
                .orderId(paymentRequest.orderId())
                .userId(paymentRequest.userId())
                .status(status)
                .timestamp(Instant.now())
                .paymentAmount(paymentRequest.paymentAmount())
                .build();

        Payment savedPayment = paymentRepository.save(payment);

        paymentEventProducer.sendPaymentCreatedEvent(payment);

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
        return paymentRepository.getTotalSumForPeriod(from, to);
    }

    @Override
    public BigDecimal getUserPaymentsByPeriod(int page, int size, Long userId, Instant from, Instant to) {
        return paymentRepository.getTotalSumForPeriodByUser(userId, from, to);
    }
}
