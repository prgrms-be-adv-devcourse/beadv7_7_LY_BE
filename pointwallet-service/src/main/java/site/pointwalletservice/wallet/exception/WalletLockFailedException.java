package site.pointwalletservice.wallet.exception;

import site.common.exception.BusinessException;
import site.pointwalletservice.wallet.exception.WalletErrorCode;

/**
 * 지갑 행에 비관적 락(NOWAIT)을 거는 시점에 다른 트랜잭션이 이미 그 지갑을 잠그고 있어 즉시 실패한 경우.
 * 데이터 문제가 아니라 순간적인 경합이라 재시도하면 될 수 있음 - 호출한 컨텍스트(Hold, Deposit 등)가
 * 자기 ErrorCode로 번역해서 다시 던진다.
 */
public class WalletLockFailedException extends BusinessException {
    public WalletLockFailedException() {
        super(WalletErrorCode.LOCK_ACQUISITION_FAILED);
    }
}