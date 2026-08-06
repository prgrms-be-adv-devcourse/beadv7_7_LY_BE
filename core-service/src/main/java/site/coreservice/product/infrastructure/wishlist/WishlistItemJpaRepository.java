package site.coreservice.product.infrastructure.wishlist;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import site.coreservice.product.domain.wishlist.WishlistItem;

import java.util.List;

public interface WishlistItemJpaRepository extends JpaRepository<WishlistItem, Long> {

    @Query("""
        select w from WishlistItem w
        where w.memberId = :memberId
          and (:cursor is null or w.id < :cursor)
        order by w.id desc
        """)
    List<WishlistItem> findAllByMemberId(@Param("memberId") Long memberId,
        @Param("cursor") Long cursor,
        Pageable pageable);

    void deleteByMemberIdAndProductId(Long memberId, Long productId);
}
