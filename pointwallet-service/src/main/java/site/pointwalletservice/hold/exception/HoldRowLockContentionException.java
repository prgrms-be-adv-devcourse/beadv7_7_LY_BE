package site.pointwalletservice.hold.exception;

/**
 * hold()에서 Hold 행(auction_id) 자체의 락 획득이 경합으로 실패한 경우 - 같은 경매에 대한 hold() 호출이
 * 거의 동시에 들어온 경우다.
 * <p>
 * HoldLockContentionException(지갑 락 경합, RetryingHoldService가 재시도함)과 이 예외를 굳이 분리한
 * 이유 - 지갑 락 경합은 "같은 유저가 서로 다른 두 경매에 동시 입찰"처럼 우리 쪽에서 조용히 재시도로
 * 흡수해도 되는 타이밍 문제지만, 이건 auction-service가 "이 hold() 요청이 지금 확정됐는지"를 즉시 알아야
 * 하는 신호다. 그래서 이 타입은 RetryingHoldService의 재시도 대상(includes)에 포함하지 않는다 - 재시도
 * 없이 즉시 실패시켜서 호출자(auction-service)가 바로 알고 자기 쪽 정책대로(재시도든 실패 처리든)
 * 판단하게 한다.
 */
public class HoldRowLockContentionException extends HoldException {
    public HoldRowLockContentionException() {
        super(HoldErrorCode.LOCK_ACQUISITION_FAILED);
    }
}