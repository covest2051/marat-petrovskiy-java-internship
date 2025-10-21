package javacore.userservice;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import userservice.dto.CardRequest;
import userservice.dto.CardResponse;
import userservice.dto.mapper.CardMapper;
import userservice.entity.Card;
import userservice.entity.User;
import userservice.exception.CardNotFoundException;
import userservice.repository.CardRepository;
import userservice.repository.UserRepository;
import userservice.service.impl.CardServiceImpl;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CardServiceImplTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CardMapper cardMapper;

    @InjectMocks
    private CardServiceImpl cardService;

    private User user;
    private Card card1;
    private Card card2;
    private CardResponse cardResponse1;
    private CardResponse cardResponse2;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        user = User.builder()
                .id(1L)
                .name("Marat")
                .surname("Petrovskiy")
                .email("marat@example.com")
                .build();

        card1 = Card.builder()
                .id(1L)
                .number("1111-2222-3333-4444")
                .holder("MARAT PIATROUSKI")
                .expirationDate(LocalDate.of(2027, 1, 1))
                .user(user)
                .build();

        card2 = Card.builder()
                .id(2L)
                .number("5555-6666-7777-8888")
                .holder("ANDREI PIATROUSKI")
                .expirationDate(LocalDate.of(2030, 1, 1))
                .user(user)
                .build();

        cardResponse1 = new CardResponse(card1.getId(), user.getId(), card1.getNumber(), card1.getHolder(), card1.getExpirationDate());
        cardResponse2 = new CardResponse(card2.getId(), user.getId(), card2.getNumber(), card2.getHolder(), card2.getExpirationDate());
    }

    @Test
    void createCard_shouldReturnCardResponse() {
        CardRequest request = new CardRequest(user.getId(), "9999-1111-2222-3333", "NEW HOLDER", LocalDate.of(2032, 1, 1));

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(cardRepository.save(any(Card.class))).thenReturn(card1);
        when(cardMapper.toCardResponse(card1)).thenReturn(cardResponse1);

        CardResponse result = cardService.createCard(request);

        assertEquals(card1.getId(), result.id());
        assertEquals(card1.getNumber(), result.number());
        verify(cardRepository).save(any(Card.class));
    }

    @Test
    void getCardById_shouldReturnCardResponse() {
        when(cardRepository.findById(1L)).thenReturn(Optional.of(card1));
        when(cardMapper.toCardResponse(card1)).thenReturn(cardResponse1);

        CardResponse result = cardService.getCardById(1L);

        assertNotNull(result);
        assertEquals(card1.getId(), result.id());
    }

    @Test
    void getCardById_shouldThrowCardNotFoundException() {
        when(cardRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(CardNotFoundException.class, () -> cardService.getCardById(99L));
    }

    @Test
    void getAllCards_shouldReturnCardResponseList() {
        when(cardRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(Arrays.asList(card1, card2)));

        when(cardMapper.toCardResponseList(Arrays.asList(card1, card2)))
                .thenReturn(Arrays.asList(cardResponse1, cardResponse2));

        List<CardResponse> result = cardService.getAllCards(0, 2);

        assertEquals(2, result.size());
        assertEquals(card1.getNumber(), result.get(0).number());
    }

    @Test
    void updateCard_shouldReturnUpdatedCard() {
        CardRequest request = new CardRequest(user.getId(), "1111-2222-3333-4444", "UPDATED CARD", LocalDate.of(2037, 1, 1));

        when(cardRepository.findById(card1.getId())).thenReturn(Optional.of(card1));
        when(cardRepository.findByNumber(request.number())).thenReturn(Optional.of(card1));
        when(cardRepository.save(any(Card.class))).thenReturn(card1);
        when(cardMapper.toCardResponse(card1)).thenReturn(cardResponse1);

        CardResponse result = cardService.updateCard(card1.getId(), request);

        assertEquals(card1.getId(), result.id());
        assertEquals("1111-2222-3333-4444", result.number());
    }

    @Test
    void deleteCard_shouldCallRepositoryDelete() {
        when(cardRepository.findById(card1.getId())).thenReturn(Optional.of(card1));

        cardService.deleteCard(card1.getId());

        verify(cardRepository, times(1)).delete(card1);
    }

}

