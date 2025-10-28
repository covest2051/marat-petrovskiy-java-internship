package userservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import userservice.dto.CardRequest;
import userservice.dto.CardResponse;
import userservice.dto.mapper.CardMapper;
import userservice.entity.Card;
import userservice.entity.User;
import userservice.exception.CardNotFoundException;
import userservice.exception.CardNumberAlreadyExistsException;
import userservice.exception.UserNotFoundException;
import userservice.repository.CardRepository;
import userservice.repository.UserRepository;
import userservice.service.CardService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CardServiceImpl implements CardService {
    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final CardMapper cardMapper;

    @Override
    @Transactional
    @Cacheable(value = "cards")
    public CardResponse createCard(CardRequest cardResponse) {
        User user = userRepository.findById(cardResponse.userId())
                .orElseThrow(() -> new UserNotFoundException("User with id " + cardResponse.userId() + "not found"));

        Card card = Card.builder()
                .number(cardResponse.number())
                .holder(cardResponse.holder())
                .expirationDate(cardResponse.expirationDate())
                .user(user)
                .build();

        Card savedCard = cardRepository.save(card);

        return cardMapper.toCardResponse(savedCard);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN') or @userSecurity.isCardOwner(#id)")
    public CardResponse getCardById(Long id) {
        Card card = cardRepository.findById(id)
                .orElseThrow(() -> new CardNotFoundException("Card with id " + id + "not found"));

        return cardMapper.toCardResponse(card);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public List<CardResponse> getAllCards(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        List<Card> cards = cardRepository.findAll(pageable).getContent();

        return cardMapper.toCardResponseList(cards);
    }

    @Override
    @Transactional
    @CachePut(value = "cards", key = "#id")
    @PreAuthorize("hasRole('ADMIN') or @userSecurity.isCardOwner(#id)")
    public CardResponse updateCard(Long id, CardRequest updatedCard) {
        Card cardToUpdate = cardRepository.findById(id)
                .orElseThrow(() -> new CardNotFoundException("Card with id " + id + " not found"));

        cardRepository.findByNumber(updatedCard.number())
                .ifPresent(existingCard -> {
                    if (!existingCard.getId().equals(id)) {
                        throw new CardNumberAlreadyExistsException("Card with number " + updatedCard.number() + " already exists");
                    }
                });

        cardToUpdate.setHolder(updatedCard.holder());
        cardToUpdate.setNumber(updatedCard.number());
        cardToUpdate.setExpirationDate(updatedCard.expirationDate());

        Card savedCard = cardRepository.save(cardToUpdate);

        return cardMapper.toCardResponse(savedCard);
    }

    @Override
    @Transactional
    @CacheEvict(value = "users", key = "#id")
    @PreAuthorize("hasRole('ADMIN') or @userSecurity.isCardOwner(#id)")
    public void deleteCard(Long id) {
        Card cardToDelete = cardRepository.findById(id)
                .orElseThrow(() -> new CardNotFoundException("Card with id " + id + " not found"));

        cardRepository.delete(cardToDelete);
    }
}
