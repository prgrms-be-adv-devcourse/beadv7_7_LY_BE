package site.pointwalletservice.wallet.presentation;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import site.common.response.ApiResponse;
import site.common.web.MemberId;
import site.pointwalletservice.shared.Money;
import site.pointwalletservice.wallet.application.WalletService;
import site.pointwalletservice.wallet.presentation.dto.WalletBalanceResponse;

/**
 * 회원 본인의 예치금 잔액 조회. 홀드는 잔액에서 즉시 차감하는 모델이라,
 * 여기서 내려주는 값이 곧 "입찰에 쓸 수 있는 금액(availableBalance)"이다.
 * <p>
 * 지갑을 아직 개설한 적 없는 회원(충전 이력 0)은 404 대신 0원으로 응답한다 — 프론트 분기 단순화 목적.
 */
@RestController
@RequestMapping("/api/v1/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @GetMapping
    public ApiResponse<WalletBalanceResponse> getBalance(@MemberId Long memberId) {
        Money balance = walletService.getBalance(memberId);
        return ApiResponse.success(WalletBalanceResponse.from(balance.getValue()));
    }
}