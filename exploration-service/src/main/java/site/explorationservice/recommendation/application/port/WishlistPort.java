package site.explorationservice.recommendation.application.port;

import java.util.List;
import site.explorationservice.recommendation.application.port.dto.WishlistProduct;

public interface WishlistPort {

    /**
     * 최근에 담은 순으로 최대 limit건을 돌려준다. product-service의 내부 API를 그대로 반영한 계약이다.
     */
    List<WishlistProduct> findRecentProducts(Long memberId, int limit);
}
