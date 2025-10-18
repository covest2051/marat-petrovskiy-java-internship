package userservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import userservice.dto.UserRequest;
import userservice.dto.UserResponse;
import userservice.dto.mapper.UserMapper;
import userservice.entity.User;
import userservice.exception.EmailAlreadyExistsException;
import userservice.exception.UserNotFoundException;
import userservice.repository.UserRepository;
import userservice.service.UserService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    public UserResponse createUser(UserRequest userToCreate) {
        if (userRepository.existsByEmail(userToCreate.email())) {
            throw new EmailAlreadyExistsException("User with email " + userToCreate.email() + " already exists");
        }

        User user = User.builder()
                .name(userToCreate.name())
                .surname(userToCreate.surname())
                .birthDate(userToCreate.birthDate())
                .email(userToCreate.email())
                .build();

        User savedUser = userRepository.save(user);

        return userMapper.toUserResponse(savedUser);
    }

    @Override
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User with id " + id + "not found"));

        return userMapper.toUserResponse(user);
    }

    @Override
    public List<UserResponse> getAllUsers(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        List<User> users = userRepository.findAll(pageable).getContent();

        return userMapper.toUserResponseList(users);
    }

    @Override
    public UserResponse getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User with email " + email + "not found"));

        return userMapper.toUserResponse(user);
    }

    @Override
    public UserResponse updateUser(Long id, UserRequest updatedUser) {
        User userToUpdate = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User with id " + id + " not found"));

        userRepository.findByEmail(updatedUser.email())
                .ifPresent(existingUser -> {
                    if (!existingUser.getId().equals(id)) {
                        throw new EmailAlreadyExistsException("User with email " + updatedUser.email() + " already exists");
                    }
                });

        userToUpdate.setName(updatedUser.name());
        userToUpdate.setSurname(updatedUser.surname());
        userToUpdate.setBirthDate(updatedUser.birthDate());
        userToUpdate.setEmail(updatedUser.email());

        User savedUser = userRepository.save(userToUpdate);

        return userMapper.toUserResponse(savedUser);
    }

    @Override
    public void deleteUser(Long id) {
        User userToDelete = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User with id " + id + " not found"));

        userRepository.delete(userToDelete);
    }
}
