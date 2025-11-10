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
import userservice.dto.UserRequest;
import userservice.dto.UserResponse;
import userservice.entity.User;
import userservice.repository.UserRepository;
import userservice.service.UserService;
import static org.assertj.core.api.Assertions.assertThat;
import java.time.LocalDate;
import java.util.Optional;

@Testcontainers
@SpringBootTest(classes = UserServiceApplication.class,
        properties = {
                "spring.cache.type=simple",
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration"
        })
public class UserServiceIntegrationTest {
    @Container
    static PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("innowisedb")
            .withUsername("postgres")
            .withPassword("postgres");

    @DynamicPropertySource
    static void setDatasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgresContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgresContainer::getUsername);
        registry.add("spring.datasource.password", postgresContainer::getPassword);
    }

    private User user;
    private UserRequest userRequest;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        user = User.builder()
                .id(1L)
                .name("Marat")
                .surname("Petrovskiy")
                .birthDate(LocalDate.of(2000, 1, 1))
                .email("marat@example.com")
                .build();

        userRequest = new UserRequest(user.getName(), user.getSurname(),
                user.getBirthDate(), user.getEmail());
    }

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void createUser_shouldSaveUser() {
        UserResponse userResponse = userService.createUser(userRequest);

        assertThat(userResponse.id()).isNotNull();
        assertThat(userResponse.name()).isEqualTo("Marat");

        assertThat(userRepository.findById(userResponse.id())).isPresent();
    }

    @Test
    void updateUser_shouldUpdateInDatabase() {
        UserResponse user = userService.createUser(userRequest);

        assertThat(user.id()).isNotNull();
        assertThat(user.name()).isEqualTo("Marat");
        assertThat(userRepository.findById(user.id())).isPresent();

        UserResponse updatedUser = userService.updateUser(
                user.id(),
                new UserRequest(this.user.getName(), this.user.getSurname(), this.user.getBirthDate(), "updated@email.com")
        );

        Optional<User> updatedUserFromDatabase = userRepository.findById(updatedUser.id());
        assertThat(updatedUserFromDatabase).isPresent();
        assertThat(updatedUserFromDatabase.get().getEmail()).isEqualTo("updated@email.com");
    }

    @Test
    void deleteUser_shouldDeleteFromDatabase() {
        UserResponse user = userService.createUser(userRequest);

        assertThat(user.id()).isNotNull();
        assertThat(user.name()).isEqualTo("Marat");
        assertThat(userRepository.findById(user.id())).isPresent();

        userService.deleteUser(user.id());

        assertThat(userRepository.findById(user.id())).isEmpty();
    }
}
