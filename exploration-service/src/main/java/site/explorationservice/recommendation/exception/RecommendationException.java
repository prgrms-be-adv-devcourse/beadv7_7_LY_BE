package site.explorationservice.recommendation.exception;

import site.common.exception.BusinessException;
import site.common.exception.ErrorCode;

/**
 * recommendation 모듈 전체가 공유하는 예외 타입. 실패 종류마다 클래스를 늘리는 대신 {@link RecommendationErrorCode}로 구분한다 —
 * 클래스가 늘어나는 만큼 새 실패가 생긴 게 아니라, 지금 있는 실패들도 대부분 "위시리스트 조회에 기댈 수 없다"는 같은 결론으로 이어지기 때문이다.
 */
public class RecommendationException extends BusinessException {

    public RecommendationException(final ErrorCode errorCode, final String message) {
        super(errorCode, message);
    }

    public RecommendationException(final ErrorCode errorCode, final String message,
        final Throwable cause) {
        super(errorCode, message, cause);
    }
}
