package site.coreservice.product.domain;

import java.text.Normalizer;
import java.util.Locale;

/**
 * 검색·중복 확인에 쓰는 표기 통일(정규화) 규칙의 단일 구현.
 * 저장할 때와 검색할 때 반드시 같은 규칙을 써야 "저장된 키와 검색 키가 어긋나는" 사고가 없으므로,
 * 정규화가 필요한 모든 곳은 이 클래스만 사용한다.
 */
public final class TextNormalizer {

    private TextNormalizer() {
    }

    /**
     * 표기를 통일한다: 전각→반각 등 호환 문자 통일(NFKC) → 소문자화(실행 환경 로케일 무관) → 글자·숫자만 남김.
     * 어떤 언어의 글자든 보존된다 (한글 포함). 남는 문자가 없으면 null을 반환한다
     * — 빈 문자열을 저장하면 "값 없음"끼리 유니크 제약에서 충돌하기 때문에 null로 통일한다.
     */
    public static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String result = Normalizer.normalize(value, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}]", "");
        return result.isEmpty() ? null : result;
    }
}
