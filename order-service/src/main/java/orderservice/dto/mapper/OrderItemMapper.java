package orderservice.dto.mapper;

import orderservice.dto.OrderItemDTO;
import orderservice.dto.ItemDTO;
import orderservice.entity.OrderItem;
import orderservice.entity.Item;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface OrderItemMapper {
    OrderItemDTO toOrderItemDTO(OrderItem orderItem);
    OrderItem toOrderItem(OrderItemDTO orderItemDTO);

    List<OrderItemDTO> toOrderItemDTOList(List<OrderItem> items);
    List<OrderItem> toOrderItemList(List<OrderItemDTO> orderItemDTOs);

    ItemDTO toItemDTO(Item item);
    Item toItem(ItemDTO itemDTO);
}
