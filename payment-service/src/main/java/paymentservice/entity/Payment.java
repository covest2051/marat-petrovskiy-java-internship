package paymentservice.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;
import java.time.Instant;

@Document(collection = "payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    private String id;

    @Indexed
    private Long orderId;

    @Indexed
    private Long userId;

    @Indexed
    private PaymentStatus status;

    private Instant timestamp;

    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal paymentAmount;
}
