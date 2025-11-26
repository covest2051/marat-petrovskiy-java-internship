package orderservice.dto.mapper;

import orderservice.entity.Order;
import orderservice.entity.OrderItem;
import orderservice.kafka.event.OrderCreatedEvent;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;
import java.util.List;

@Mapper(componentModel = "spring")
public interface OrderEventMapper {
    @Mapping(target = "paymentAmount", expression = "java(calculateAmount(order.getOrderItems()))")
    OrderCreatedEvent toOrderCreatedEvent(Order order);

    default BigDecimal calculateAmount(List<OrderItem> items) {
        return items.stream()
                .map(item -> item.getItem().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
