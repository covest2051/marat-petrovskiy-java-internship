package paymentservice.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;

// TODO: Изменить id в Payment на Long,
@Document(collection = "payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    private Long id;

    @Indexed
    private Long orderId;

    @Indexed
    private Long userId;

    @Indexed
    private PaymentStatus status;

    private Instant timestamp;

    private BigDecimal paymentAmount;
}
