package authenticationservice.repository;

import authenticationservice.entity.KeycloakUserMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface KeycloakUserMappingRepository extends JpaRepository<KeycloakUserMapping, Long> {

    Optional<KeycloakUserMapping> findByLogin(String login);

    Optional<KeycloakUserMapping> findByKeycloakId(String keycloakId);

    boolean existsByLogin(String login);
}