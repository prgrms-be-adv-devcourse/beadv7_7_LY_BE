package site.pointwalletservice.withdraw.exception;

/**
 * requestWithdraw()에서 사용자 본인 지갑 락 획득이 경합으로 실패한 경우. 잔액부족/지갑없음 등
 * 다른 WithdrawException과 달리 이건 "재시도하면 풀릴 수 있는" 실패라, RetryingWithdrawService가
 * 이 타입만 골라서 재시도한다.
 * <p>
 * 플랫폼 계정(수수료 적립) 락 경합은 이 예외 대상이 아니다 - 수수료 적립은 더 이상 이 트랜잭션
 * 안에서 동기로 처리하지 않고 WithdrawFeeEarnedEvent 발행으로 넘기기 때문에(WithdrawApplicationService
 * 주석 참고), 여기서 남는 건 "같은 유저가 자기 지갑을 동시에 건드리는" 사용자 단위 경합뿐이다.
 * 이건 Hold 도메인에서 검증한 것과 같은 성격의(정상 시나리오 동시경합 2~5 수준) 문제라 재시도로
 * 충분하다고 판단했다.
 */
public class WithdrawLockContentionException extends WithdrawException {
    public WithdrawLockContentionException() {
        super(WithdrawErrorCode.LOCK_ACQUISITION_FAILED);
    }
}