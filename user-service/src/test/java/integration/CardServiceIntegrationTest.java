package integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import userservice.UserServiceApplication;
import userservice.dto.CardRequest;
import userservice.dto.CardResponse;
import userservice.dto.UserRequest;
import userservice.dto.UserResponse;
import userservice.entity.Card;
import userservice.entity.User;
import userservice.repository.CardRepository;
import userservice.repository.UserRepository;
import userservice.service.CardService;
import userservice.service.UserService;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(classes = UserServiceApplication.class,
        properties = {
                "spring.cache.type=simple",
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration"
        })
public class CardServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("innowisedb")
            .withUsername("postgres")
            .withPassword("postgres");

    @DynamicPropertySource
    static void registerPgProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private CardService cardService;

    @Autowired
    private UserService userService;

    @Autowired
    private CardRepository cardRepository;
    @Autowired
    private UserRepository userRepository;

    private UserResponse createdUser;

    private User user;
    private Card card;
    private CardRequest cardRequest;


    @BeforeEach
    void setUp() {
        cardRepository.deleteAll();
        userRepository.deleteAll();

        user = User.builder()
                .id(1L)
                .name("Marat")
                .surname("Petrovskiy")
                .birthDate(LocalDate.of(2000, 1, 1))
                .email("maratpetrovitch@gmail.com")
                .build();

        this.createdUser = userService.createUser(new UserRequest(user.getName(), user.getSurname(),
                user.getBirthDate(), user.getEmail()));

        this.cardRequest = new CardRequest(
                createdUser.id(),
                "1111-2222-3333-4444",
                "MARAT PIATROUSKI",
                LocalDate.of(2027, 1, 1)
        );

        this.card = Card.builder()
                .id(1L)
                .number("1111-2222-3333-4444")
                .holder("MARAT PIATROUSKI")
                .expirationDate(LocalDate.of(2027, 1, 1))
                .user(user)
                .build();
    }

    @Test
    void createCard_shouldSaveCard() {
        CardResponse createdCard = cardService.createCard(cardRequest);

        assertThat(createdCard.id()).isNotNull();
        assertThat(createdCard.number()).isEqualTo(cardRequest.number());
        assertThat(createdCard.holder()).isEqualTo(cardRequest.holder());

        Optional<Card> cardFromDatabase = cardRepository.findById(createdCard.id());
        assertThat(cardFromDatabase).isPresent();
        assertThat(cardFromDatabase.get().getNumber()).isEqualTo(cardRequest.number());
    }

    @Test
    void updateCard_integration_shouldPersistChanges() {
        CardResponse createdCard = cardService.createCard(cardRequest);

        CardRequest update = new CardRequest(
                createdUser.id(),
                "1111-2222-3333-4444",
                "UPDATED HOLDER",
                LocalDate.of(2037, 1, 1)
        );

        CardResponse updatedCard = cardService.updateCard(createdCard.id(), update);

        assertThat(updatedCard).isNotNull();
        assertThat(updatedCard.id()).isEqualTo(createdCard.id());
        assertThat(updatedCard.holder()).isEqualTo("UPDATED HOLDER");
        assertThat(updatedCard.expirationDate()).isEqualTo(update.expirationDate());

        Optional<userservice.entity.Card> cardFromDb = cardRepository.findById(createdCard.id());
        assertThat(cardFromDb).isPresent();
        cardFromDb.ifPresent(c -> {
            assertThat(c.getHolder()).isEqualTo("UPDATED HOLDER");
            assertThat(c.getExpirationDate()).isEqualTo(update.expirationDate());
            assertThat(c.getNumber()).isEqualTo(createdCard.number());
        });
    }

    @Test
    void deleteCard_shouldRemoveCardFromDatabase() {
        CardResponse createdCard = cardService.createCard(cardRequest);

        assertThat(cardRepository.findById(createdCard.id())).isPresent();

        cardService.deleteCard(createdCard.id());

        assertThat(cardRepository.findById(createdCard.id())).isEmpty();
    }
}
