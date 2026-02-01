package paymentservice.dto.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import paymentservice.entity.Payment;
import paymentservice.kafka.event.PaymentCreatedEvent;

@Mapper(componentModel = "spring")
public interface PaymentEventMapper {
    @Mapping(source = "id", target = "paymentId")
    @Mapping(source = "orderId", target = "orderId")
    @Mapping(source = "userId", target = "userId")
    @Mapping(source = "status", target = "status")
    @Mapping(source = "timestamp", target = "timestamp")
    @Mapping(source = "paymentAmount", target = "paymentAmount")
    PaymentCreatedEvent toPaymentCreatedEvent(Payment payment);
}
