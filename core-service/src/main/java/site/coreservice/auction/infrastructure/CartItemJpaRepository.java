package site.coreservice.auction.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import site.coreservice.auction.domain.CartItem;

import java.util.List;

public interface CartItemJpaRepository extends JpaRepository<CartItem, Long> {

    List<CartItem> findAllByMemberId(Long memberId);
}
