package site.productservice.application.wishlist;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.productservice.application.dto.wishlist.WishlistItemPage;
import site.productservice.domain.wishlist.WishlistItem;
import site.productservice.domain.wishlist.WishlistItemRepository;

@Service
@RequiredArgsConstructor
public class WishlistService {

    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final WishlistItemRepository wishlistItemRepository;

    @Transactional
    protected WishlistItem add(final Long memberId, final Long productId) {
        return wishlistItemRepository.save(WishlistItem.of(memberId, productId));
    }

    @Transactional
    protected void remove(final Long memberId, final Long productId) {
        wishlistItemRepository.deleteByMemberIdAndProductId(memberId, productId);
    }

    /**
     * 커서 페이징으로 한 페이지를 읽는다. 다음 페이지가 있는지 알아내려고 요청받은 size보다 한 건 더 조회한 뒤, 넘치면 잘라내고 hasNext를 세운다.
     */
    @Transactional(readOnly = true)
    protected WishlistItemPage findPage(final Long memberId, final Long cursor, final int size) {
        final int pageSize = clampSize(size);
        final List<WishlistItem> fetched = wishlistItemRepository.findAllByMemberId(memberId,
            cursor, pageSize + 1);

        final boolean hasNext = fetched.size() > pageSize;
        final List<WishlistItem> items = hasNext ? fetched.subList(0, pageSize) : fetched;
        final Long nextCursor = hasNext ? items.getLast().getId() : null;

        return new WishlistItemPage(items, nextCursor, hasNext);
    }

    // 한 번에 너무 많이 긁어가지 않도록 저장소에 물어보기 전에 막는다.
    private int clampSize(final int size) {
        if (size < 1) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }
}
