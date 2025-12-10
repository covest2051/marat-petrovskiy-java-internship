package orderservice.dto.mapper;

import orderservice.dto.OrderResponse;
import orderservice.dto.UserResponse;
import orderservice.entity.Order;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", uses = {OrderItemMapper.class})
public interface OrderMapper {
    @Mapping(target = "userResponse", source = "user")
    OrderResponse toOrderResponse(Order order, UserResponse user);

    List<OrderResponse> toOrderResponseList(List<Order> orders);
}
