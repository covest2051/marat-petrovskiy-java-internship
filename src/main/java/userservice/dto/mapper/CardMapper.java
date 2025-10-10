package userservice.dto.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import userservice.dto.UserResponse;
import userservice.entity.Card;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface CardMapper {
    UserResponse toCardResponse(Card card);

    List<UserResponse> toCardResponseList(List<Card> cards);
}
