package paymentservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import paymentservice.dto.PaymentRequest;
import paymentservice.dto.PaymentResponse;
import paymentservice.entity.PaymentStatus;
import paymentservice.service.PaymentService;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(@Valid @RequestBody PaymentRequest request) {
        PaymentResponse createdPayment = paymentService.createPayment(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(createdPayment);
    }

    @GetMapping("/order/{id}")
    public ResponseEntity<List<PaymentResponse>> getPaymentsByOrderId(@RequestParam(defaultValue = "0") int page,
                                                                      @RequestParam(defaultValue = "10") int size,
                                                                      @PathVariable Long id) {
        return ResponseEntity.ok(paymentService.getPaymentsByOrderId(page, size, id));
    }

    @GetMapping("/user/{id}")
    public ResponseEntity<List<PaymentResponse>> getPaymentsByUserId(@RequestParam(defaultValue = "0") int page,
                                                                     @RequestParam(defaultValue = "10") int size,
                                                                     @PathVariable Long id) {
        return ResponseEntity.ok(paymentService.getPaymentsByUserId(page, size, id));
    }

    @GetMapping("/status")
    public ResponseEntity<List<PaymentResponse>> getPaymentsByStatus(@RequestParam(defaultValue = "0") int page,
                                                                     @RequestParam(defaultValue = "10") int size,
                                                                     @RequestParam("status") PaymentStatus status) {
        return ResponseEntity.ok(paymentService.getPaymentsByStatus(page, size, status));
    }

    @GetMapping("/total")
    public ResponseEntity<BigDecimal> getAllPaymentsByPeriod(@RequestParam(defaultValue = "0") int page,
                                                             @RequestParam(defaultValue = "10") int size,
                                                             @RequestParam("from") Instant from,
                                                             @RequestParam("to") Instant to) {
        return ResponseEntity.ok(paymentService.getAllPaymentsByPeriod(page, size, from, to));
    }

    @GetMapping("/total/user/{id}")
    public ResponseEntity<BigDecimal> getUserPaymentsByPeriod(@RequestParam(defaultValue = "0") int page,
                                                              @RequestParam(defaultValue = "10") int size,
                                                              @PathVariable Long id,
                                                              @RequestParam("from") Instant from,
                                                              @RequestParam("to") Instant to) {
        return ResponseEntity.ok(paymentService.getUserPaymentsByPeriod(page, size, id, from, to));
    }
}
