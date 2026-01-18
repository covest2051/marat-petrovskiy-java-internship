package userservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import userservice.dto.UserInternalDto;
import userservice.entity.User;
import userservice.repository.UserRepository;

@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
public class InternalUserController {
    private final UserRepository userRepository;

    @PostMapping
    public void createUserProfile(@RequestBody UserInternalDto dto) {
        User user = new User();
        user.setId(dto.id());
        user.setEmail(dto.email());
        user.setName(dto.name());
        user.setSurname(dto.surname());
        userRepository.save(user);
    }
}
