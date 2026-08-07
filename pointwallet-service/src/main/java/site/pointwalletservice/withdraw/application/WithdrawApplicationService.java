package site.pointwalletservice.withdraw.application;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import site.pointwalletservice.ledger.application.PointTransactionService;
import site.pointwalletservice.ledger.domain.PointTransactionType;
import site.pointwalletservice.shared.Money;
import site.pointwalletservice.shared.PlatformAccount;
import site.pointwalletservice.wallet.application.WalletBalanceResult;
import site.pointwalletservice.wallet.application.WalletService;
import site.pointwalletservice.wallet.domain.InsufficientBalanceException;
import site.pointwalletservice.wallet.exception.WalletNotFoundException;
import site.pointwalletservice.withdraw.application.dto.WithdrawRequestResult;
import site.pointwalletservice.withdraw.application.dto.WithdrawStatusResult;
import site.pointwalletservice.withdraw.application.port.BankAccount;
import site.pointwalletservice.withdraw.application.port.MemberBankAccountPort;
import site.pointwalletservice.withdraw.domain.Withdraw;
import site.pointwalletservice.withdraw.domain.WithdrawRepository;
import site.pointwalletservice.withdraw.exception.WithdrawErrorCode;
import site.pointwalletservice.withdraw.exception.WithdrawException;

@Service
@RequiredArgsConstructor
public class WithdrawApplicationService implements WithdrawService {

    private static final BigDecimal WITHDRAW_FEE_RATE = BigDecimal.valueOf(0.02);

    private final WithdrawRepository withdrawRepository;
    private final WalletService walletService;
    private final PointTransactionService pointTransactionService;
    private final MemberBankAccountPort memberBankAccountPort;

    @Override
    @Transactional
    public WithdrawRequestResult requestWithdraw(Long userId, Money amount) {
        // 1) 계좌 유효성 확인 (저장은 안 함 - 그때그때 조회만)
        BankAccount bankAccount = memberBankAccountPort.getBankAccount(userId)
                .orElseThrow(() -> new WithdrawException(WithdrawErrorCode.BANK_ACCOUNT_NOT_FOUND));

        // 2) 수수료 계산 (2%, 내림) - PLATFORM_USER_ID 본인은 이 플로우 대상이 아니므로 예외 처리 불필요
        Money feeAmount = amount.multiply(WITHDRAW_FEE_RATE);
        Money netAmount = amount.subtract(feeAmount);

        // 3) 신청 금액 전액을 사용자 지갑에서 차감
        WalletBalanceResult userResult;
        try {
            userResult = walletService.deduct(userId, amount);
        } catch (WalletNotFoundException e) {
            throw new WithdrawException(WithdrawErrorCode.WALLET_NOT_FOUND);
        } catch (InsufficientBalanceException e) {
            throw new WithdrawException(WithdrawErrorCode.INSUFFICIENT_BALANCE);
        }

        Withdraw withdraw = withdrawRepository.save(Withdraw.request(userId, amount, feeAmount, netAmount));

        pointTransactionService.record(
                userResult.walletId(), PointTransactionType.WITHDRAW, amount, userResult.balanceAfter(), withdraw.getId()
        );

        // 4) 수수료를 플랫폼 계정으로 적립
        WalletBalanceResult platformResult = walletService.charge(PlatformAccount.PLATFORM_USER_ID, feeAmount);
        pointTransactionService.record(
                platformResult.walletId(), PointTransactionType.FEE_INCOME, feeAmount,
                platformResult.balanceAfter(), withdraw.getId()
        );

        // 5) 실제 뱅킹 연동 없이 즉시 성공 처리 (시뮬레이션). bankAccount는 이번 스텝에서 검증 용도로만 사용.
        withdraw.complete();

        return WithdrawRequestResult.from(withdraw);
    }

    @Override
    @Transactional(readOnly = true)
    public WithdrawStatusResult getStatus(Long withdrawRequestId) {
        Withdraw withdraw = withdrawRepository.findById(withdrawRequestId)
                .orElseThrow(() -> new WithdrawException(WithdrawErrorCode.WITHDRAW_NOT_FOUND));
        return WithdrawStatusResult.from(withdraw);
    }
}