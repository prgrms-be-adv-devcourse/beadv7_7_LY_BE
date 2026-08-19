package site.explorationservice.productindex.application;

import java.util.Arrays;
import java.util.stream.Collectors;
import site.explorationservice.productindex.application.dto.ProductIndexCommand;

/**
 * 상품을 임베딩할 텍스트로 조립한다.
 * <b>값은 코드가 아니라 사람이 쓰는 말로 바꿔 넣는다</b> — 임베딩 모델은 자연어로 학습돼서 enum 이름이나
 * 연도 숫자는 신호가 약하다. ORIGINAL은 "오리지널 프레스"로, 1959는 "1950년대"로 바꾼다. 숫자 연도는 범위 검색용이지 의미의 재료가 아니다.
 * <p>
 * 두 형식을 두는 건 <b>어느 쪽이 나은지 확립된 결론이 없어서</b>다. 키-값은 모호성을 없애고(Columbia가 레이블인지 국가인지), 나열은 학습 분포에 가깝다.
 * 레이블 접두어는 모든 상품에 똑같이 들어가 상대 순위에는 거의 영향이 없으므로 토큰 낭비라고 볼 것도 아니다. 상품 전체를 각각 색인해 추천 결과로 비교한다.
 * <p>
 * <b>바꾸면 전체 재임베딩이 필요하다</b>(OpenAI 재호출 비용·시간). 다만 상품 100건 규모에서는 몇 원
 * 수준이라 지금이 실험하기 가장 싼 시점이다.
 */
public enum ProductEmbeddingTemplate {

    /**
     * <pre>
     * Miles Davis · Jazz · Columbia
     * 1950년대 · 미국 · 오리지널 프레스
     * </pre>
     */
    COMPACT {
        @Override
        public String build(final ProductIndexCommand command) {
            final String identity = joinNonBlank(" · ",
                command.artistName(), command.genre(), command.label());
            final String release = joinNonBlank(" · ",
                decade(command.releaseYear()), command.releaseCountry(),
                pressType(command.pressType()));

            return joinNonBlank("\n", identity, release);
        }
    },

    /**
     * <pre>
     * 아티스트: Miles Davis
     * 장르: Jazz · 레이블: Columbia
     * 1950년대 · 미국 · 오리지널 프레스
     * </pre>
     */
    LABELED {
        @Override
        public String build(final ProductIndexCommand command) {
            final String artist = labeled("아티스트", command.artistName());
            final String classification = joinNonBlank(" · ",
                labeled("장르", command.genre()), labeled("레이블", command.label()));
            final String release = joinNonBlank(" · ",
                decade(command.releaseYear()), command.releaseCountry(),
                pressType(command.pressType()));

            return joinNonBlank("\n", artist, classification, release);
        }
    };

    private static final String ORIGINAL = "ORIGINAL";
    private static final String REISSUE = "REISSUE";

    public abstract String build(ProductIndexCommand command);

    /**
     * 3-벡터 구조(identity·origin·edition)용 보조 텍스트. COMPACT/LABELED 구분과 무관하게 항상 나열형으로 만든다 — 두 필드짜리 조합은
     * 라벨이 없어도 모호할 일이 거의 없어서(예: "Kraftwerk · Electronic"), {@link #build}처럼 형식을 두고 고민할 이유가 없다. 근거는
     * docs/search-recommendation-design-notes.md "클러스터링 · 가중치 최종 목표 아키텍처" 참고.
     */
    public static String buildIdentity(final ProductIndexCommand command) {
        return joinNonBlank(" · ", command.genre(), command.artistName());
    }

    public static String buildOrigin(final ProductIndexCommand command) {
        return joinNonBlank(" · ", decade(command.releaseYear()), command.releaseCountry());
    }

    /**
     * pressType은 다른 3-벡터 텍스트와 달리 한국어로 바꾸지 않고 원본 값(ORIGINAL/REISSUE)을 그대로 쓴다 — {@link #pressType}
     * 변환은 {@link #build}(combined)에만 적용된다.
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

    private static String labeled(final String label, final String value) {
        return (value == null || value.isBlank()) ? null : label + ": " + value;
    }

    /**
     * 1959 -> "1950년대". 숫자 자체는 의미가 없지만 연대는 사람이 인식하는 개념이라 벡터가 잡아낸다.
     */
    private static String decade(final Integer releaseYear) {
        return releaseYear == null ? null : (releaseYear / 10 * 10) + "년대";
    }

    /**
     * 모르는 값이 오면 원본을 그대로 둔다 — 조용히 지워서 정보를 잃는 것보다 낫다.
     */
    private static String pressType(final String pressType) {
        if (pressType == null || pressType.isBlank()) {
            return null;
        }
        return switch (pressType) {
            case ORIGINAL -> "오리지널 프레스";
            case REISSUE -> "재발매반";
            default -> pressType;
        };
    }
}
