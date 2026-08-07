package site.auctionservice.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import site.auctionservice.domain.CartItem;

import java.util.List;

public interface CartItemJpaRepository extends JpaRepository<CartItem, Long> {

    List<CartItem> findAllByMemberId(Long memberId);

    boolean existsByMemberIdAndAuctionId(Long memberId, Long auctionId);

    long countByMemberId(Long memberId);

    void deleteByMemberIdAndAuctionId(Long memberId, Long auctionId);
}
