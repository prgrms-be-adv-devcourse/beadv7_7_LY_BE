package site.coreservice.pointwallet.hold.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import site.coreservice.pointwallet.hold.domain.Hold;
import site.coreservice.pointwallet.hold.domain.HoldRepository;
import site.coreservice.pointwallet.hold.exception.HoldErrorCode;
import site.coreservice.pointwallet.hold.exception.HoldException;
import site.coreservice.pointwallet.ledger.application.PointTransactionService;
import site.coreservice.pointwallet.ledger.domain.PointTransactionType;
import site.coreservice.pointwallet.shared.Money;
import site.coreservice.pointwallet.wallet.application.WalletBalanceResult;
import site.coreservice.pointwallet.wallet.application.WalletService;
import site.coreservice.pointwallet.wallet.domain.InsufficientBalanceException;
import site.coreservice.pointwallet.wallet.exception.WalletNotFoundException;

@ExtendWith(MockitoExtension.class)
@DisplayName("HoldApplicationService")
class HoldApplicationServiceTest {

    @Mock
    private HoldRepository holdRepository;

    @Mock
    private WalletService walletService;

    @Mock
    private PointTransactionService pointTransactionService;

    private HoldApplicationService sut;

    private static final Long AUCTION_ID = 5001L;
    private static final Long BIDDER_ID = 456L;
    private static final Long WALLET_ID = 100L;
    private static final Money AMOUNT = Money.of(15_000);

    @BeforeEach
    void setUp() {
        sut = new HoldApplicationService(holdRepository, walletService, pointTransactionService);
    }

    @Nested
    @DisplayName("기존 활성 홀드가 없는 경우")
    class NoPreviousHold {

        @Test
        @DisplayName("새 입찰자 지갑에서 차감하고 홀드를 생성한다 - releasedHoldId는 null")
        void hold_기존홀드없으면_새로_홀드생성() {
            // given
            Long newHoldId = 999L;
            when(holdRepository.findByAuctionId(AUCTION_ID)).thenReturn(Optional.empty());
            when(walletService.deduct(BIDDER_ID, AMOUNT)).thenReturn(new WalletBalanceResult(WALLET_ID, Money.of(85_000)));
            when(holdRepository.save(any(Hold.class))).thenAnswer(invocation -> {
                Hold hold = invocation.getArgument(0);
                org.springframework.test.util.ReflectionTestUtils.setField(hold, "id", newHoldId);
                return hold;
            });

            // when
            HoldResult result = sut.hold(AUCTION_ID, BIDDER_ID, AMOUNT);

            // then
            assertThat(result.holdId()).isEqualTo(newHoldId);
            assertThat(result.releasedHoldId()).isNull();
            assertThat(result.balanceAfter()).isEqualTo(Money.of(85_000));

            ArgumentCaptor<Hold> holdCaptor = ArgumentCaptor.forClass(Hold.class);
            verify(holdRepository).save(holdCaptor.capture());
            assertThat(holdCaptor.getValue().getAuctionId()).isEqualTo(AUCTION_ID);
            assertThat(holdCaptor.getValue().getUserId()).isEqualTo(BIDDER_ID);

            verify(pointTransactionService).record(
                    WALLET_ID, PointTransactionType.HOLD, AMOUNT, Money.of(85_000), newHoldId
            );
        }

        @Test
        @DisplayName("지갑이 없으면 HOLD 컨텍스트의 WALLET_NOT_FOUND로 번역된다")
        void hold_지갑이_없으면_WALLET_NOT_FOUND() {
            // given
            when(holdRepository.findByAuctionId(AUCTION_ID)).thenReturn(Optional.empty());
            when(walletService.deduct(BIDDER_ID, AMOUNT)).thenThrow(new WalletNotFoundException());

            // when & then
            assertThatThrownBy(() -> sut.hold(AUCTION_ID, BIDDER_ID, AMOUNT))
                    .isInstanceOf(HoldException.class)
                    .extracting(e -> ((HoldException) e).getErrorCode())
                    .isEqualTo(HoldErrorCode.WALLET_NOT_FOUND);

            verify(holdRepository, never()).save(any(Hold.class));
            verify(pointTransactionService, never()).record(any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("잔액이 부족하면 HOLD 컨텍스트의 INSUFFICIENT_BALANCE로 번역된다")
        void hold_잔액부족이면_INSUFFICIENT_BALANCE() {
            // given
            when(holdRepository.findByAuctionId(AUCTION_ID)).thenReturn(Optional.empty());
            when(walletService.deduct(BIDDER_ID, AMOUNT)).thenThrow(new InsufficientBalanceException());

            // when & then
            assertThatThrownBy(() -> sut.hold(AUCTION_ID, BIDDER_ID, AMOUNT))
                    .isInstanceOf(HoldException.class)
                    .extracting(e -> ((HoldException) e).getErrorCode())
                    .isEqualTo(HoldErrorCode.INSUFFICIENT_BALANCE);

            verify(holdRepository, never()).save(any(Hold.class));
            verify(pointTransactionService, never()).record(any(), any(), any(), any(), any());
        }
    }

    @Nested
    @DisplayName("기존 활성 홀드가 있는 경우 (다른 유저의 이전 최고 입찰)")
    class WithPreviousHold {

        private static final Long PREVIOUS_BIDDER_ID = 789L;
        private static final Long PREVIOUS_HOLD_ID = 1L;
        private static final Long PREVIOUS_WALLET_ID = 200L;
        private static final Money PREVIOUS_AMOUNT = Money.of(10_000);

        @Test
        @DisplayName("이전 홀드를 해제(환입)하고 새 홀드를 생성한다 - releasedHoldId가 채워진다")
        void hold_기존홀드있으면_해제하고_새로_홀드생성() {
            // given
            Long newHoldId = 999L;
            Hold previousHold = Hold.place(AUCTION_ID, PREVIOUS_BIDDER_ID, PREVIOUS_AMOUNT);
            org.springframework.test.util.ReflectionTestUtils.setField(previousHold, "id", PREVIOUS_HOLD_ID);
            when(holdRepository.findByAuctionId(AUCTION_ID)).thenReturn(Optional.of(previousHold));

            when(walletService.credit(PREVIOUS_BIDDER_ID, PREVIOUS_AMOUNT))
                    .thenReturn(new WalletBalanceResult(PREVIOUS_WALLET_ID, PREVIOUS_AMOUNT));
            when(walletService.deduct(BIDDER_ID, AMOUNT))
                    .thenReturn(new WalletBalanceResult(WALLET_ID, Money.of(85_000)));
            when(holdRepository.save(any(Hold.class))).thenAnswer(invocation -> {
                Hold hold = invocation.getArgument(0);
                org.springframework.test.util.ReflectionTestUtils.setField(hold, "id", newHoldId);
                return hold;
            });

            // when
            HoldResult result = sut.hold(AUCTION_ID, BIDDER_ID, AMOUNT);

            // then
            assertThat(result.holdId()).isEqualTo(newHoldId);
            assertThat(result.releasedHoldId()).isEqualTo(PREVIOUS_HOLD_ID);
            assertThat(result.balanceAfter()).isEqualTo(Money.of(85_000));

            // then: 이전 입찰자 지갑에 환입 위임 + 원장기록(RELEASE) + 홀드 레코드 삭제
            verify(walletService).credit(PREVIOUS_BIDDER_ID, PREVIOUS_AMOUNT);
            verify(pointTransactionService).record(
                    PREVIOUS_WALLET_ID, PointTransactionType.RELEASE, PREVIOUS_AMOUNT, PREVIOUS_AMOUNT, PREVIOUS_HOLD_ID
            );
            verify(holdRepository).delete(previousHold);

            // then: 새 입찰자 지갑 차감 + 원장기록(HOLD)
            verify(walletService).deduct(BIDDER_ID, AMOUNT);
            verify(pointTransactionService).record(
                    WALLET_ID, PointTransactionType.HOLD, AMOUNT, Money.of(85_000), newHoldId
            );
        }

        @Test
        @DisplayName("이전 입찰자 지갑이 없으면(데이터 정합성 문제) 자동 개설하지 않고 WALLET_NOT_FOUND를 던진다")
        void hold_이전입찰자_지갑이_없으면_자동개설하지_않고_예외() {
            // given
            Hold previousHold = Hold.place(AUCTION_ID, PREVIOUS_BIDDER_ID, PREVIOUS_AMOUNT);
            when(holdRepository.findByAuctionId(AUCTION_ID)).thenReturn(Optional.of(previousHold));
            when(walletService.credit(PREVIOUS_BIDDER_ID, PREVIOUS_AMOUNT)).thenThrow(new WalletNotFoundException());

            // when & then
            assertThatThrownBy(() -> sut.hold(AUCTION_ID, BIDDER_ID, AMOUNT))
                    .isInstanceOf(HoldException.class)
                    .extracting(e -> ((HoldException) e).getErrorCode())
                    .isEqualTo(HoldErrorCode.WALLET_NOT_FOUND);

            // 이전 홀드 해제가 실패했으니 새 입찰자 쪽 차감·홀드 생성까지 가면 안 된다
            verify(walletService, never()).deduct(anyLong(), any());
            verify(holdRepository, never()).save(any(Hold.class));
            verify(holdRepository, never()).delete(any(Hold.class));
        }
    }
}