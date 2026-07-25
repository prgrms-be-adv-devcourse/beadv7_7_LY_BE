package site.coreservice.order.infrastructure;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import site.coreservice.order.domain.Order;
import site.coreservice.order.domain.OrderRepository;
import site.coreservice.order.domain.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class OrderRepositoryImpl implements OrderRepository {

    private final OrderJpaRepository orderJpaRepository;

    @Override
    public Order save(Order order) {
        return orderJpaRepository.save(order);
    }

    @Override
    public Optional<Order> findById(Long id) {
        return orderJpaRepository.findById(id);
    }

    @Override
    public boolean existsByAuctionId(Long auctionId) {
        return orderJpaRepository.existsByAuctionId(auctionId);
    }

    @Override
    public List<Order> findAllByStatusAndOrderDeadlineBefore(OrderStatus status, LocalDateTime threshold) {
        return orderJpaRepository.findAllByStatusAndOrderDeadlineBefore(status, threshold);
    }

    @Override
    public List<Order> findAllByStatusAndCompletionDeadlineBefore(OrderStatus status, LocalDateTime threshold) {
        return orderJpaRepository.findAllByStatusAndCompletionDeadlineBefore(status, threshold);
    }
}
