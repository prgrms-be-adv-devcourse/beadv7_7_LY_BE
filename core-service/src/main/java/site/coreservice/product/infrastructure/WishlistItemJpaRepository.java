package site.coreservice.product.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import site.coreservice.product.domain.WishlistItem;

import java.util.List;

public interface WishlistItemJpaRepository extends JpaRepository<WishlistItem, Long> {

    List<WishlistItem> findAllByMemberId(Long memberId);
}
