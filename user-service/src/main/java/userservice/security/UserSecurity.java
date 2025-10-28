package userservice.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import userservice.repository.CardRepository;
import userservice.repository.UserRepository;

@Component
@RequiredArgsConstructor
public class UserSecurity {
    private final UserRepository userRepository;
    private final CardRepository cardRepository;

    public boolean isUserOwner(Long userId) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;
        if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) return true;
        String login = auth.getName();
        return userRepository.findById(userId)
                .map(u -> u.getEmail().equals(login))
                .orElse(false);
    }

    public boolean isCardOwner(Long cardId) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;
        if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) return true;
        String login = auth.getName();
        return cardRepository.findById(cardId)
                .map(c -> c.getUser() != null && login.equals(c.getUser().getEmail()))
                .orElse(false);
    }
}
