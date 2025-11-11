package orderservice.repository;

import orderservice.entity.Order;
import orderservice.entity.OrderStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findAllByUserId(Long userId, Pageable pageable);

    List<Order> findAllByStatus(OrderStatus status, Pageable pageable);
}
