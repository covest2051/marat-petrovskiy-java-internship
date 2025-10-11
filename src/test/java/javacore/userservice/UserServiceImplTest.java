package javacore.userservice;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import userservice.dto.UserRequest;
import userservice.dto.UserResponse;
import userservice.dto.mapper.UserMapper;
import userservice.entity.User;
import userservice.exception.EmailAlreadyExistsException;
import userservice.exception.UserNotFoundException;
import userservice.repository.UserRepository;
import userservice.service.UserService;
import userservice.service.impl.UserServiceImpl;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceImplTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserServiceImpl userService;

    private User user1;
    private User user2;
    private UserResponse userResponse1;
    private UserResponse userResponse2;


    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        user1 = User.builder()
                .id(1L)
                .name("Marat")
                .surname("Petrovskiy")
                .birthDate(LocalDate.of(2000, 1, 1))
                .email("marat@example.com")
                .build();

        user2 = User.builder()
                .id(2L)
                .name("Andrey")
                .surname("Petrovskiy")
                .birthDate(LocalDate.of(2001, 1, 1))
                .email("andrey@example.com")
                .build();

        userResponse1 = new UserResponse(user1.getId(), user1.getName(), user1.getSurname(),
                user1.getBirthDate(), user1.getEmail(), Collections.emptyList());

        userResponse2 = new UserResponse(user2.getId(), user2.getName(), user2.getSurname(),
                user2.getBirthDate(), user2.getEmail(), Collections.emptyList());
    }

    @Test
    void createUser_shouldReturnUserResponse() {
        UserRequest request = new UserRequest("Marat",
                "Petrovskiy",
                LocalDate.of(2006, 1, 1),
                "maratpetrovitch@gmail.com");

        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(user1);
        when(userMapper.toUserResponse(user1)).thenReturn(userResponse1);

        UserResponse result = userService.createUser(request);

        assertEquals(user1.getId(), result.id());
        assertEquals(user1.getEmail(), result.email());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void createUser_shouldThrowEmailAlreadyExistsException() {
        UserRequest request = new UserRequest(
                "Marat",
                "Petrovskiy",
                LocalDate.of(2000, 1, 1),
                "maratpetrovitch@gmail.com");

        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        Assertions.assertThrows(EmailAlreadyExistsException.class, () -> userService.createUser(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void getUserById_shouldReturnUserResponse() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));
        when(userMapper.toUserResponse(user1)).thenReturn(userResponse1);

        UserResponse result = userService.getUserById(1L);

        assertNotNull(result);
        assertEquals(user1.getId(), result.id());
        assertEquals(user1.getName(), result.name());

        verify(userRepository, times(1)).findById(1L);
    }

    @Test
    void getAllUsers_shouldReturnUserResponseList() {
        when(userRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(Arrays.asList(user1, user2)));
        when(userMapper.toUserResponseList(Arrays.asList(user1, user2)))
                .thenReturn(Arrays.asList(userResponse1, userResponse2));

        List<UserResponse> result = userService.getAllUsers(0, 2);

        assertEquals(2, result.size());
        assertEquals(user1.getName(), result.get(0).name());
        assertEquals(user2.getName(), result.get(1).name());
    }

    @Test
    void getUserByEmail_shouldReturnUserResponse() {
        when(userRepository.findByEmail(user1.getEmail())).thenReturn(Optional.of(user1));
        when(userMapper.toUserResponse(user1)).thenReturn(userResponse1);

        UserResponse result = userService.getUserByEmail(user1.getEmail());

        assertNotNull(result);
        assertEquals(user1.getId(), result.id());
        assertEquals(user1.getName(), result.name());

        verify(userRepository, times(1)).findByEmail(user1.getEmail());
    }

    @Test
    void updateUser_shouldReturnUserResponse() {
        UserRequest request = new UserRequest("Marat",
                "Petrovskiy",
                LocalDate.of(2006, 1, 1),
                "maratpetrovskiy@gmail.com");

        when(userRepository.findById(user1.getId())).thenReturn(Optional.of(user1));
        when(userRepository.findByEmail(request.email())).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(user1);
        when(userMapper.toUserResponse(user1)).thenReturn(userResponse1);

        UserResponse result = userService.updateUser(user1.getId(), request);

        assertNotNull(result);
        assertEquals(user1.getId(), result.id());
        assertEquals(user1.getName(), result.name());
        assertEquals("maratpetrovskiy@gmail.com", request.email());
    }

    @Test
    void deleteUser_shouldDeleteExistingUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user1));

        userService.deleteUser(1L);

        verify(userRepository, times(1)).delete(user1);
    }

    @Test
    void deleteUser_shouldThrowExceptionWhenUserNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        Assertions.assertThrows(UserNotFoundException.class, () -> userService.deleteUser(1L));

        verify(userRepository, never()).delete(any());
    }
}
