package site.explorationservice.productindex.application;

import java.util.Arrays;
import java.util.stream.Collectors;
import site.explorationservice.productindex.application.dto.ProductIndexCommand;

/**
 * 상품을 identity·origin·edition 3벡터용 텍스트로 조립한다.
 * <p>
 * <b>값은 코드가 아니라 사람이 쓰는 말로 바꿔 넣는다</b> — 임베딩 모델은 자연어로 학습돼서 연도 숫자는 신호가 약하다. 1959는 "1950년대"로
 * 바꾼다. 숫자 연도는 범위 검색용이지 의미의 재료가 아니다.
 * <p>
 */
public final class ProductEmbeddingTemplate {

    private ProductEmbeddingTemplate() {
    }

    /**
     * 장르 + 아티스트("음악적 정체성"). 근거는 docs/search-recommendation-design-notes.md "클러스터링 · 가중치 최종 목표 아키텍처"
     * 참고.
     */
    public static String buildIdentity(final ProductIndexCommand command) {
        return joinNonBlank(" · ", command.genre(), command.artistName());
    }

    public static String buildOrigin(final ProductIndexCommand command) {
        return joinNonBlank(" · ", decade(command.releaseYear()), command.releaseCountry());
    }

    /**
     * pressType은 identity·origin과 달리 한국어로 바꾸지 않고 원본 값(ORIGINAL/REISSUE)을 그대로 쓴다 — edition 축은 정확한
     * 카테고리 일치가 중요하다.
     */
    public static String buildEdition(final ProductIndexCommand command) {
        return joinNonBlank(" · ", command.label(), command.pressType());
    }

    /**
     * 값이 비어 있는 필드는 통째로 빠진다 — "장르: null" 같은 문구가 벡터에 섞이면 안 된다.
     */
    private static String joinNonBlank(final String delimiter, final String... parts) {
        return Arrays.stream(parts)
            .filter(part -> part != null && !part.isBlank())
            .collect(Collectors.joining(delimiter));
    }

    /**
     * 1959 -> "1950년대". 숫자 자체는 의미가 없지만 연대는 사람이 인식하는 개념이라 벡터가 잡아낸다.
     */
    private static String decade(final Integer releaseYear) {
        return releaseYear == null ? null : (releaseYear / 10 * 10) + "년대";
    }
}
