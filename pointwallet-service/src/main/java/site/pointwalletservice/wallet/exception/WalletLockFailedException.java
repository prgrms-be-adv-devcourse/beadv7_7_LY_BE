package site.pointwalletservice.wallet.exception;

import site.common.exception.BusinessException;
import site.pointwalletservice.wallet.exception.WalletErrorCode;

/**
 * 지갑 행 락 대기가 끝내 실패한 경우(타임아웃 등) - 지갑 락은 기본적으로 기다리므로(NOWAIT 아님) 정상
 * 흐름에서는 거의 발생하지 않고, 발생하면 트랜잭션이 비정상적으로 오래 걸리고 있다는 신호에 가깝다.
 * 호출한 컨텍스트(Hold, Deposit 등)가 자기 ErrorCode로 번역해서 다시 던진다.
 */
public class WalletLockFailedException extends BusinessException {
    public WalletLockFailedException() {
        super(WalletErrorCode.LOCK_ACQUISITION_FAILED);
    }
}