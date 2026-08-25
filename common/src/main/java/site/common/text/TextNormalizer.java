package site.common.text;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 검색·중복 확인에 쓰는 표기 통일(정규화) 규칙의 단일 구현.
 * 저장할 때와 검색할 때 반드시 같은 규칙을 써야 "저장된 키와 검색 키가 어긋나는" 사고가 없으므로,
 * 정규화가 필요한 모든 곳은 이 클래스만 사용한다.
 * <p>
 * 상품을 저장하는 서비스와 검색어를 다듬는 서비스가 갈라져 있어 공통 모듈에 둔다.
 * <p>
 * 결과가 컬럼에 저장돼 있으므로, 규칙을 바꾸면 저장된 값도 함께 다시 계산해야 한다.
 */
public final class TextNormalizer {

    // 고정 정규식은 한 번만 컴파일해 재사용 — 호출마다 다시 컴파일하지 않도록
    private static final Pattern NON_LETTER_OR_DIGIT = Pattern.compile("[^\\p{L}\\p{N}]");

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
        String result = NON_LETTER_OR_DIGIT
                .matcher(Normalizer.normalize(value, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT))
                .replaceAll("");
        return result.isEmpty() ? null : result;
    }
}
