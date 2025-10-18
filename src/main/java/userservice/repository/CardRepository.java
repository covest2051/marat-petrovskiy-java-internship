package userservice.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import userservice.entity.Card;
import userservice.entity.User;

import java.awt.print.Pageable;
import java.util.List;
import java.util.Optional;

@Repository
public interface CardRepository extends JpaRepository<Card, Long> {
    List<Card> findByUser(User user);

    @Query("SELECT c FROM Card c WHERE c.number LIKE %:number%")
    List<Page<Card>> findByNumberContaining(String number, Pageable pageable);

    @Query(value = "SELECT * FROM cards WHERE number = :number", nativeQuery = true)
    Optional<Card> findByNumberNative(String number);

    Optional<Card> findByNumber(String number);
}
