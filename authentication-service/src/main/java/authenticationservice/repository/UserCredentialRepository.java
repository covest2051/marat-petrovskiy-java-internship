package authenticationservice.repository;

import authenticationservice.entity.UserCredential;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserCredentialRepository extends JpaRepository<UserCredential, Long> {
    Optional<UserCredential> findByLogin(String login);
    boolean existsByLogin(String login);
}
