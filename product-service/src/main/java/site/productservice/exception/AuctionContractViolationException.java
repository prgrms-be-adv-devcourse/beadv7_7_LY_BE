package site.productservice.exception;

import site.common.exception.BusinessException;

/**
 * 경매 조회 응답이 우리가 기대한 형태와 다를 때. 값이 잘못 온 것이라 다시 물어봐도 같은 답이 온다 —
 * 재시도 대상인 일시 장애(5xx·타임아웃)와 반드시 구분해야 해서 별도 타입으로 둔다.
 * 어긋난 값 자체를 메시지에 담는다. 로그만 보고 경매 API 어디가 다른지 알 수 있어야 하기 때문.
 * <p>
 * ⚠️ 이 예외는 <b>리스너 방벽 안에서만 쓰는 것을 전제</b>한다. 상태코드가 500이고 메시지에 내부 사정이
 * 그대로 담기므로, 사용자 요청 경로(예: 카탈로그 조회에서 경매 수를 함께 가져오는 후속 작업)에서
 * 같은 클라이언트를 쓰면 공통 예외 처리기를 타고 그대로 사용자에게 나간다. 그 경로에서 쓰려면
 * 호출하는 쪽에서 잡아 사용자에게 보일 형태로 바꾸거나, 값이 없을 때의 대체 동작을 정해야 한다.
 */
public class AuctionContractViolationException extends BusinessException {

    public AuctionContractViolationException(String detail) {
        super(ProductErrorCode.AUCTION_CONTRACT_VIOLATION, "경매 조회 응답이 계약과 다릅니다 — " + detail);
    }
}
