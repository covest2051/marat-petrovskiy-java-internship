package paymentservice.dto.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import paymentservice.dto.PaymentResponse;
import paymentservice.entity.Payment;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PaymentMapper {

    PaymentResponse toPaymentResponse(Payment payment);

    List<PaymentResponse> toPaymentResponseList(List<Payment> payments);
}

