package userservice.service;

import userservice.dto.UserRequest;
import userservice.dto.UserResponse;

import java.util.List;

public interface UserService {

    UserResponse createUser(UserRequest userRequest);

    UserResponse getUserById(Long id);

    List<UserResponse> getAllUsers(int page, int size);

    UserResponse getUserByEmail(String email);

    UserResponse updateUser(Long id, UserRequest userRequest);

    void deleteUser(Long id);
}
