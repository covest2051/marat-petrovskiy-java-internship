package userservice.dto.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import userservice.dto.CardResponse;
import userservice.entity.Card;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CardMapper {
    CardResponse toCardResponse(Card card);

    List<CardResponse> toCardResponseList(List<Card> cards);
}
