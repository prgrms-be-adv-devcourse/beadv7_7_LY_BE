package site.coreservice.pointwallet.wallet.application;

import java.util.Optional;
import site.coreservice.pointwallet.shared.Money;

public interface WalletService {

    WalletBalanceResult charge(Long userId, Money amount);

    WalletBalanceResult credit(Long userId, Money amount);

    WalletBalanceResult deduct(Long userId, Money amount);

    /** 거래내역 조회처럼 지갑 존재 자체가 불확실한(자동 개설하면 안 되는) 읽기 전용 조회용. */
    Optional<Long> findWalletId(Long userId);

    /**
     * 회원 본인의 잔액 조회용. 지갑이 없으면 Money.zero() — "지갑 없음"과 "잔액 0원"을
     * 프론트에서 구분할 필요가 없는 조회 API(마이페이지 지갑 탭 등)에 쓴다.
     */
    Money getBalance(Long userId);
}