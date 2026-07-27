package site.coreservice.order.infrastructure;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import site.coreservice.order.domain.Order;
import site.coreservice.order.domain.OrderRepository;
import site.coreservice.order.domain.OrderSearchPage;
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

    @Override
    public OrderSearchPage findAllByBuyerId(Long buyerId, OrderStatus status, int page, int size) {
        Page<Order> result = orderJpaRepository.searchByBuyerId(buyerId, status, PageRequest.of(page, size));
        return new OrderSearchPage(result.getContent(), result.getTotalElements());
    }

    @Override
    public OrderSearchPage findAllBySellerId(Long sellerId, OrderStatus status, int page, int size) {
        Page<Order> result = orderJpaRepository.searchBySellerId(sellerId, status, PageRequest.of(page, size));
        return new OrderSearchPage(result.getContent(), result.getTotalElements());
    }
}
