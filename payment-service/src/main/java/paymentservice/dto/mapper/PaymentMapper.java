package paymentservice.dto.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import paymentservice.dto.PaymentResponse;
import paymentservice.entity.Payment;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PaymentMapper {
    @Mapping(source = "id", target = "id")
    @Mapping(source = "orderId", target = "orderId")
    @Mapping(source = "userId", target = "userId")
    @Mapping(source = "status", target = "status")
    @Mapping(source = "timestamp", target = "timestamp")
    @Mapping(source = "paymentAmount", target = "paymentAmount")
    PaymentResponse toPaymentResponse(Payment payment);

    List<PaymentResponse> toPaymentResponseList(List<Payment> payments);
}

