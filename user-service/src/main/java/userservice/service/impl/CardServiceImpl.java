package userservice.service.impl;

import io.micrometer.core.instrument.Counter;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@Service
@RequiredArgsConstructor
@Observed(name = "userservice.cards")
public class CardServiceImpl implements CardService {
    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final CardMapper cardMapper;

    private final Counter cardCreatedCounter;
    private final Counter cardNotFoundCounter;

    @Override
    @Transactional
    @Cacheable(value = "cards")
    @Observed(name = "userservice.cards.create", contextualName = "create-card")
    public CardResponse createCard(CardRequest cardRequest) {
        log.info("Creating card for userId={} number={}",
                cardRequest.userId(), hideCardNumber(cardRequest.number()));

        User user = userRepository.findById(cardRequest.userId())
                .orElseThrow(() -> new UserNotFoundException(
                        "User with id " + cardRequest.userId() + " not found"));

        Card card = Card.builder()
                .number(cardRequest.number())
                .holder(cardRequest.holder())
                .expirationDate(cardRequest.expirationDate())
                .user(user)
                .build();

        Card savedCard = cardRepository.save(card);

        cardCreatedCounter.increment();
        log.info("Card created successfully id={} userId={}", savedCard.getId(), user.getId());

        return cardMapper.toCardResponse(savedCard);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN') or @userSecurity.isCardOwner(#id)")
    @Observed(name = "userservice.cards.get-by-id", contextualName = "get-card-by-id")
    public CardResponse getCardById(Long id) {
        log.debug("Fetching card id={}", id);

        Card card = cardRepository.findById(id).orElseThrow(() -> {
            cardNotFoundCounter.increment();
            log.warn("Card not found id={}", id);
            return new CardNotFoundException("Card with id " + id + " not found");
        });
        return cardMapper.toCardResponse(card);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Observed(name = "userservice.cards.get-all", contextualName = "get-all-cards")
    public List<CardResponse> getAllCards(int page, int size) {
        log.debug("Fetching all cards page={} size={}", page, size);

        Pageable pageable = PageRequest.of(page, size);
        return cardMapper.toCardResponseList(cardRepository.findAll(pageable).getContent());
    }

    @Override
    @Transactional
    @CachePut(value = "cards", key = "#id")
    @PreAuthorize("hasRole('ADMIN') or @userSecurity.isCardOwner(#id)")
    @Observed(name = "userservice.cards.update", contextualName = "update-card")
    public CardResponse updateCard(Long id, CardRequest updatedCard) {
        log.info("Updating card id={}", id);

        Card cardToUpdate = cardRepository.findById(id).orElseThrow(() -> {
            cardNotFoundCounter.increment();
            return new CardNotFoundException("Card with id " + id + " not found");
        });

        cardRepository.findByNumber(updatedCard.number()).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new CardNumberAlreadyExistsException(
                        "Card with number " + hideCardNumber(updatedCard.number()) + " already exists");
            }
        });

        cardToUpdate.setHolder(updatedCard.holder());
        cardToUpdate.setNumber(updatedCard.number());
        cardToUpdate.setExpirationDate(updatedCard.expirationDate());

        Card savedCard = cardRepository.save(cardToUpdate);
        log.info("Card updated successfully id={}", savedCard.getId());
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

    // Подумал, что безопаснее не предоставлять номера карт в логах :)
    private String hideCardNumber(String number) {
        if (number == null || number.length() <= 4) return "****";
        return "*".repeat(number.length() - 4) + number.substring(number.length() - 4);
    }
}
