package site.auctionservice.exception;

/**
 * 지갑 hold() 호출이 연결 이후 단계(요청 전송/응답 대기 중)에서 실패해, 실제로 처리됐는지 여부를 알 수 없는 상태임을 나타낸다.
 * WalletHttpClient가 ResourceAccessException의 원인을 연결 단계 실패(안전, 재시도 대상)와  구분하기 위해 던진다.
 * resilience4j retry의 ignoreExceptions에 등록돼 있어 재시도되지 않는다.
 */
public class WalletHoldOutcomeUnknownException extends RuntimeException {
    public WalletHoldOutcomeUnknownException(Throwable cause) {
        super(cause);
    }
}
