package site.coreservice.pointwallet.wallet.application;

import java.util.Optional;
import site.coreservice.pointwallet.shared.Money;

public interface WalletService {

    WalletBalanceResult charge(Long userId, Money amount);

    WalletBalanceResult credit(Long userId, Money amount);

    WalletBalanceResult deduct(Long userId, Money amount);

    /** 거래내역 조회처럼 지갑 존재 자체가 불확실한(자동 개설하면 안 되는) 읽기 전용 조회용. */
    Optional<Long> findWalletId(Long userId);
}