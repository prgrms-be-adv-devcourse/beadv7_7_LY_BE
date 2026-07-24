package site.coreservice.order.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import site.coreservice.order.domain.Order;

public interface OrderJpaRepository extends JpaRepository<Order, Long> {

    boolean existsByAuctionId(Long auctionId);
}
