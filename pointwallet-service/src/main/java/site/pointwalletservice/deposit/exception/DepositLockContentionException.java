package site.pointwalletservice.deposit.exception;

/**
 * confirmDeposit()의 DB 반영 단계에서 지갑 락 획득이 경합으로 실패한 경우. 같은 orderId에 대한
 * 중복 확정 요청(예: PG 콜백 중복 전달)이나, 같은 유저의 다른 지갑 작업과 동시에 몰렸을 때 발생한다.
 * <p>
 * 이 경합은 "PG 승인은 이미 성공했는데 DB 반영만 실패한 것"과는 성격이 다르다 - 승인이 멱등
 * (Idempotency-Key)하므로 뒤늦게 온 요청이 재시도하면 자연스럽게 풀리는 일시적 실패다. 그래서
 * DepositApplicationService는 이 예외를 잡으면 보정 취소(cancel)를 태우지 않고, RetryingDepositService가
 * confirmDeposit() 호출 자체를 처음부터 재시도한다.
 */
public class DepositLockContentionException extends DepositException {
    public DepositLockContentionException() {
        super(DepositErrorCode.LOCK_ACQUISITION_FAILED);
    }
}