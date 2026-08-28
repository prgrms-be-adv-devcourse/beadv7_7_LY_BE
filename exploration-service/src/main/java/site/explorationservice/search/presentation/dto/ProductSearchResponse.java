package site.explorationservice.search.presentation.dto;

import java.util.List;
import site.explorationservice.search.application.dto.ProductSearchResult;
import site.explorationservice.search.domain.ProductSearchHit;

/**
 * 상품 서비스의 검색 응답과 형식이 같다.
 */
public record ProductSearchResponse(List<Card> content, int page, int size, long totalElements, boolean hasNext,
                                    String searchId) {

    public static ProductSearchResponse from(final ProductSearchResult result) {
        final List<Card> cards = result.content().stream()
                .map(Card::from)
                .toList();
        return new ProductSearchResponse(cards, result.page(), result.size(), result.totalElements(),
                result.hasNext(), result.searchId());
    }

    /**
     * releaseYear 만 상품 서비스의 int 가 아니라 Integer 다 (나중에 product쪽 변경 필요). 표시 필드는 내부 API 확장과 재색인이 끝나야 채워지는데, 그전까지 0을 지어내면
     * 화면에 0년으로 표시될 수 있다. 상품 테이블의 발매연도는 값이 반드시 있으므로 재색인 후에는 나가는 JSON이 기존과 같아진다.
     */
    public record Card(Long productId, String title, String artistName, String coverImageUrl, Integer releaseYear,
                       String pressType) {

        public static Card from(final ProductSearchHit hit) {
            return new Card(
                    hit.productId(),
                    hit.title(),
                    hit.artistName(),
                    hit.coverImageUrl(),
                    hit.releaseYear(),
                    hit.pressType());
        }
    }
}
