package userservice.service;

import userservice.dto.CardRequest;
import userservice.dto.CardResponse;

import java.util.List;

public interface CardService {

    CardResponse createCard(CardRequest cardRequest);

    CardResponse getCardById(Long id);

    List<CardResponse> getAllCards(int page, int size);

    CardResponse updateCard(Long id, CardRequest cardRequest);

    void deleteCard(Long id);
}
