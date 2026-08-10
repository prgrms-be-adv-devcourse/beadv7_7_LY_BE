package site.pointwalletservice.hold.exception;

/**
 * hold()에서 Hold 행 락 또는 지갑 락 획득이 경합으로 실패한 경우. 잔액부족/지갑없음 등 다른
 * HoldException과 달리 이건 "재시도하면 풀릴 수 있는" 실패라, RetryingHoldService가 이 타입만
 * 골라서 재시도한다.
 */
public class HoldLockContentionException extends HoldException {
    public HoldLockContentionException() {
        super(HoldErrorCode.LOCK_ACQUISITION_FAILED);
    }
}