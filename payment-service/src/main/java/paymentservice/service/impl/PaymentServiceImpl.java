package paymentservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.support.SendResult;
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
import paymentservice.kafka.event.PaymentCreatedEvent;
import paymentservice.repository.PaymentRepository;
import paymentservice.service.PaymentService;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

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
        Optional<Payment> existingPayment = paymentRepository.findByOrderId(paymentRequest.orderId())
                .stream()
                .filter(p -> p.getStatus() == PaymentStatus.CREATED)
                .findFirst();

        if (existingPayment.isPresent()) {
            return paymentMapper.toPaymentResponse(existingPayment.get());
        }

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
        Optional<Payment> existingPayment = paymentRepository.findByOrderId(event.orderId())
                .stream()
                .filter(p -> p.getStatus() == PaymentStatus.CREATED)
                .findFirst();

        if (existingPayment.isPresent()) {
            return paymentMapper.toPaymentResponse(existingPayment.get());
        }

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
        List<Payment> payments = paymentRepository.findByOrderId(orderId, pageable).getContent();

        return paymentMapper.toPaymentResponseList(payments);
    }

    @Override
    public List<PaymentResponse> getPaymentsByUserId(int page, int size, Long userId) {
        Pageable pageable = PageRequest.of(page, size);
        List<Payment> payments = paymentRepository.findAllByUserId(userId, pageable).getContent();

        return paymentMapper.toPaymentResponseList(payments);
    }

    @Override
    public List<PaymentResponse> getPaymentsByStatus(int page, int size, PaymentStatus status) {
        Pageable pageable = PageRequest.of(page, size);
        List<Payment> payments = paymentRepository.findAllByStatus(status, pageable).getContent();

        return paymentMapper.toPaymentResponseList(payments);
    }

    @Override
    public BigDecimal getAllPaymentsByPeriod(int page, int size, Instant from, Instant to) {
        List<Payment> payments = paymentRepository.findByTimestampBetweenAndStatus(from, to, PaymentStatus.CREATED);
        return payments.stream()
                .map(Payment::getPaymentAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public BigDecimal getUserPaymentsByPeriod(int page, int size, Long userId, Instant from, Instant to) {
        List<Payment> payments = paymentRepository.findByUserIdAndTimestampBetweenAndStatus(userId, from, to, PaymentStatus.CREATED);
        return payments.stream()
                .map(Payment::getPaymentAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
