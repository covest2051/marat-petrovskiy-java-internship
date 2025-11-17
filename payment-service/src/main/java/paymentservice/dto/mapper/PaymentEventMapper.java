package paymentservice.dto.mapper;

import org.mapstruct.Mapper;
import paymentservice.entity.Payment;
import paymentservice.kafka.event.PaymentCreatedEvent;

@Mapper(componentModel = "spring")
public interface PaymentEventMapper {
    PaymentCreatedEvent toPaymentCreatedEvent(Payment payment);
}
