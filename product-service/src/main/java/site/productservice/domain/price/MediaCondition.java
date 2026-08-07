package site.productservice.domain.price;

/**
 * 미디어(음반 자체) 컨디션 6등급. 경매·주문 도메인의 등급 체계와 같은 값을 쓴다 —
 * 다른 도메인의 enum을 직접 가져오지 않는 팀 규칙 때문에 사본으로 둔다.
 * 표기는 풀네임 대문자 고정 (M/NM 같은 축약은 화면 표시에서만).
 */
public enum MediaCondition {

    MINT, NEAR_MINT, VERY_GOOD_PLUS, VERY_GOOD, GOOD, POOR;

    public static MediaCondition from(String value) {
        if (value == null) {
            throw new IllegalArgumentException("컨디션 등급이 없습니다");
        }
        try {
            return valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("알 수 없는 컨디션 등급입니다: " + value);
        }
    }
}
