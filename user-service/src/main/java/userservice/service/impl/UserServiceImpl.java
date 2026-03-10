package userservice.service.impl;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
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
import userservice.dto.UserRequest;
import userservice.dto.UserResponse;
import userservice.dto.mapper.UserMapper;
import userservice.entity.User;
import userservice.exception.EmailAlreadyExistsException;
import userservice.exception.UserNotFoundException;
import userservice.repository.UserRepository;
import userservice.service.UserService;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Observed(name = "userservice.users")
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    private final Counter userCreatedCounter;
    private final Counter userNotFoundCounter;
    private final Timer userLookupTimer;

    @Override
    @Transactional
    @Observed(name = "userservice.users.create", contextualName = "create-user")
    public UserResponse createUser(UserRequest userToCreate) {
        log.info("Creating user with email={}", userToCreate.email());

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

        userCreatedCounter.increment();
        log.info("User created successfully id={} email={}", savedUser.getId(), savedUser.getEmail());

        return userMapper.toUserResponse(savedUser);
    }

    @Override
    @Cacheable(value = "users", key = "#id")
    @PreAuthorize("hasRole('ADMIN') or @userSecurity.isUserOwner(#id)")
    @Observed(name = "userservice.users.get-by-id", contextualName = "get-user-by-id")
    public UserResponse getUserById(Long id) {
        log.debug("Fetching user id={}", id);

        return userLookupTimer.record(() -> {
            User user = userRepository.findById(id).orElseThrow(() -> {
                userNotFoundCounter.increment();
                log.warn("User not found id={}", id);
                return new UserNotFoundException("User with id " + id + " not found");
            });
            return userMapper.toUserResponse(user);
        });
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    @Observed(name = "userservice.users.get-all", contextualName = "get-all-users")
    public List<UserResponse> getAllUsers(int page, int size) {
        log.debug("Fetching all users page={} size={}", page, size);

        Pageable pageable = PageRequest.of(page, size);
        List<User> users = userRepository.findAll(pageable).getContent();

        return userMapper.toUserResponseList(users);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN') or #email == authentication.name")
    @Observed(name = "userservice.users.get-by-email", contextualName = "get-user-by-email")
    public UserResponse getUserByEmail(String email) {
        log.debug("Fetching user email={}", email);

        User user = userRepository.findByEmail(email).orElseThrow(() -> {
            userNotFoundCounter.increment();
            log.warn("User not found email={}", email);
            return new UserNotFoundException("User with email " + email + " not found");
        });
        return userMapper.toUserResponse(user);
    }

    @Override
    @Transactional
    @CachePut(value = "users", key = "#id")
    @PreAuthorize("hasRole('ADMIN') or @userSecurity.isUserOwner(#id)")
    @Observed(name = "userservice.users.update", contextualName = "update-user")
    public UserResponse updateUser(Long id, UserRequest updatedUser) {
        log.info("Updating user id={}", id);

        User userToUpdate = userRepository.findById(id).orElseThrow(() -> {
            userNotFoundCounter.increment();
            return new UserNotFoundException("User with id " + id + " not found");
        });

        userRepository.findByEmail(updatedUser.email()).ifPresent(existingUser -> {
            if (!existingUser.getId().equals(id)) {
                throw new EmailAlreadyExistsException(
                        "User with email " + updatedUser.email() + " already exists");
            }
        });

        userToUpdate.setName(updatedUser.name());
        userToUpdate.setSurname(updatedUser.surname());
        userToUpdate.setBirthDate(updatedUser.birthDate());
        userToUpdate.setEmail(updatedUser.email());

        User savedUser = userRepository.save(userToUpdate);

        log.info("User updated successfully id={}", savedUser.getId());

        return userMapper.toUserResponse(savedUser);
    }

    @Override
    @Transactional
    @CacheEvict(value = "users", key = "#id")
    @PreAuthorize("hasRole('ADMIN') or @userSecurity.isUserOwner(#id)")
    @Observed(name = "userservice.users.delete", contextualName = "delete-user")
    public void deleteUser(Long id) {
        log.info("Deleting user id={}", id);

        User userToDelete = userRepository.findById(id).orElseThrow(() -> {
            userNotFoundCounter.increment();
            return new UserNotFoundException("User with id " + id + " not found");
        });

        userRepository.delete(userToDelete);
        log.info("User deleted successfully id={}", id);
    }
}
