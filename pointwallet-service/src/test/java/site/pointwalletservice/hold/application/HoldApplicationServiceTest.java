package site.pointwalletservice.hold.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;
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
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.test.util.ReflectionTestUtils;
import site.pointwalletservice.hold.domain.Hold;
import site.pointwalletservice.hold.domain.HoldRepository;
import site.pointwalletservice.hold.exception.HoldErrorCode;
import site.pointwalletservice.hold.exception.HoldException;
import site.pointwalletservice.ledger.application.PointTransactionService;
import site.pointwalletservice.ledger.domain.PointTransactionType;
import site.pointwalletservice.shared.Money;
import site.pointwalletservice.wallet.application.WalletBalanceResult;
import site.pointwalletservice.wallet.application.WalletService;
import site.pointwalletservice.wallet.domain.InsufficientBalanceException;
import site.pointwalletservice.wallet.exception.WalletLockFailedException;
import site.pointwalletservice.wallet.exception.WalletNotFoundException;


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
            when(holdRepository.findByAuctionIdForUpdate(AUCTION_ID)).thenReturn(Optional.empty());
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
            when(holdRepository.findByAuctionIdForUpdate(AUCTION_ID)).thenReturn(Optional.empty());
            when(walletService.deduct(BIDDER_ID, AMOUNT)).thenThrow(new WalletNotFoundException());

            // when & then
            assertThatThrownBy(() -> sut.hold(AUCTION_ID, BIDDER_ID, AMOUNT))
                    .isInstanceOf(HoldException.class)
                    .extracting(e -> ((HoldException) e).getErrorCode())
                    .isEqualTo(HoldErrorCode.INSUFFICIENT_BALANCE);

            verify(holdRepository, never()).save(any(Hold.class));
            verify(pointTransactionService, never()).record(any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("잔액이 부족하면 HOLD 컨텍스트의 INSUFFICIENT_BALANCE로 번역된다")
        void hold_잔액부족이면_INSUFFICIENT_BALANCE() {
            // given
            when(holdRepository.findByAuctionIdForUpdate(AUCTION_ID)).thenReturn(Optional.empty());
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
    @DisplayName("락 경합 (hold row / 지갑 락 획득 실패)")
    class LockContention {

        @Test
        @DisplayName("Hold 행 락 획득에 실패하면(동시 입찰 경합) LOCK_ACQUISITION_FAILED로 번역된다 - 지갑은 건드리지 않는다")
        void hold_Hold행_락획득실패시_LOCK_ACQUISITION_FAILED() {
            // given
            when(holdRepository.findByAuctionIdForUpdate(AUCTION_ID))
                    .thenThrow(new PessimisticLockingFailureException("lock not available"));

            // when & then
            assertThatThrownBy(() -> sut.hold(AUCTION_ID, BIDDER_ID, AMOUNT))
                    .isInstanceOf(HoldException.class)
                    .extracting(e -> ((HoldException) e).getErrorCode())
                    .isEqualTo(HoldErrorCode.LOCK_ACQUISITION_FAILED);

            verify(walletService, never()).lockForUpdate(anyLong(), any());
            verify(walletService, never()).deduct(anyLong(), any());
        }

        @Test
        @DisplayName("지갑 락 획득에 실패하면 LOCK_ACQUISITION_FAILED로 번역된다 - 차감까지 가지 않는다")
        void hold_지갑락_획득실패시_LOCK_ACQUISITION_FAILED() {
            // given
            when(holdRepository.findByAuctionIdForUpdate(AUCTION_ID)).thenReturn(Optional.empty());
            org.mockito.Mockito.doThrow(new WalletLockFailedException())
                    .when(walletService).lockForUpdate(BIDDER_ID, null);

            // when & then
            assertThatThrownBy(() -> sut.hold(AUCTION_ID, BIDDER_ID, AMOUNT))
                    .isInstanceOf(HoldException.class)
                    .extracting(e -> ((HoldException) e).getErrorCode())
                    .isEqualTo(HoldErrorCode.LOCK_ACQUISITION_FAILED);

            verify(walletService, never()).deduct(anyLong(), any());
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
            when(holdRepository.findByAuctionIdForUpdate(AUCTION_ID)).thenReturn(Optional.of(previousHold));

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
            when(holdRepository.findByAuctionIdForUpdate(AUCTION_ID)).thenReturn(Optional.of(previousHold));
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

        @Test
        @DisplayName("이전 입찰자 지갑 환입 중 락 획득에 실패하면 LOCK_ACQUISITION_FAILED로 번역된다")
        void hold_이전홀드_환입중_락획득실패시_LOCK_ACQUISITION_FAILED() {
            // given
            Hold previousHold = Hold.place(AUCTION_ID, PREVIOUS_BIDDER_ID, PREVIOUS_AMOUNT);
            when(holdRepository.findByAuctionIdForUpdate(AUCTION_ID)).thenReturn(Optional.of(previousHold));
            when(walletService.credit(PREVIOUS_BIDDER_ID, PREVIOUS_AMOUNT))
                    .thenThrow(new WalletLockFailedException());

            // when & then
            assertThatThrownBy(() -> sut.hold(AUCTION_ID, BIDDER_ID, AMOUNT))
                    .isInstanceOf(HoldException.class)
                    .extracting(e -> ((HoldException) e).getErrorCode())
                    .isEqualTo(HoldErrorCode.LOCK_ACQUISITION_FAILED);

            verify(walletService, never()).deduct(anyLong(), any());
            verify(holdRepository, never()).save(any(Hold.class));
            verify(holdRepository, never()).delete(any(Hold.class));
        }
    }

    @Nested
    @DisplayName("홀드 해제 (release) - 주문 취소 대응")
    class Release {

        @Test
        @DisplayName("활성 홀드가 있으면 지갑에 환원하고 RELEASE 원장 기록 후 삭제한다")
        void release_정상흐름() {
            // given
            Hold hold = Hold.place(AUCTION_ID, BIDDER_ID, AMOUNT);
            ReflectionTestUtils.setField(hold, "id", 1L);
            when(holdRepository.findByAuctionIdForUpdate(AUCTION_ID)).thenReturn(Optional.of(hold));
            when(walletService.credit(BIDDER_ID, AMOUNT))
                    .thenReturn(new WalletBalanceResult(WALLET_ID, AMOUNT));

            // when
            sut.release(AUCTION_ID);

            // then
            verify(walletService).credit(BIDDER_ID, AMOUNT);
            verify(pointTransactionService).record(
                    WALLET_ID, PointTransactionType.RELEASE, AMOUNT, AMOUNT, 1L
            );
            verify(holdRepository).delete(hold);
        }

        @Test
        @DisplayName("해제할 홀드가 없으면 예외 없이 조용히 종료한다 (트랜잭션 rollback-only 방지)")
        void release_홀드없으면_예외없이_스킵() {
            // given
            when(holdRepository.findByAuctionIdForUpdate(AUCTION_ID)).thenReturn(Optional.empty());

            // when & then
            assertThatCode(() -> sut.release(AUCTION_ID)).doesNotThrowAnyException();

            verify(walletService, never()).credit(any(), any());
            verify(holdRepository, never()).delete(any());
            verify(pointTransactionService, never()).record(any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("Hold 행 락 획득에 실패하면 LOCK_ACQUISITION_FAILED로 번역된다")
        void release_락획득실패시_LOCK_ACQUISITION_FAILED() {
            // given
            when(holdRepository.findByAuctionIdForUpdate(AUCTION_ID))
                    .thenThrow(new PessimisticLockingFailureException("lock not available"));

            // when & then
            assertThatThrownBy(() -> sut.release(AUCTION_ID))
                    .isInstanceOf(HoldException.class)
                    .extracting(e -> ((HoldException) e).getErrorCode())
                    .isEqualTo(HoldErrorCode.LOCK_ACQUISITION_FAILED);

            verify(walletService, never()).credit(any(), any());
        }
    }

    @Nested
    @DisplayName("홀드 소멸 (consume) - 주문 완료 대응")
    class Consume {

        @Test
        @DisplayName("활성 홀드가 있으면 지갑은 건드리지 않고 삭제만 한다")
        void consume_정상흐름() {
            // given
            Hold hold = Hold.place(AUCTION_ID, BIDDER_ID, AMOUNT);
            when(holdRepository.findByAuctionIdForUpdate(AUCTION_ID)).thenReturn(Optional.of(hold));

            // when
            sut.consume(AUCTION_ID);

            // then
            verify(holdRepository).delete(hold);
            verify(walletService, never()).credit(any(), any());
            verify(walletService, never()).deduct(any(), any());
            verify(pointTransactionService, never()).record(any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("소멸시킬 홀드가 없으면 예외 없이 조용히 종료한다 (트랜잭션 rollback-only 방지)")
        void consume_홀드없으면_예외없이_스킵() {
            // given
            when(holdRepository.findByAuctionIdForUpdate(AUCTION_ID)).thenReturn(Optional.empty());

            // when & then
            assertThatCode(() -> sut.consume(AUCTION_ID)).doesNotThrowAnyException();

            verify(holdRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Hold 행 락 획득에 실패하면 LOCK_ACQUISITION_FAILED로 번역된다")
        void consume_락획득실패시_LOCK_ACQUISITION_FAILED() {
            // given
            when(holdRepository.findByAuctionIdForUpdate(AUCTION_ID))
                    .thenThrow(new PessimisticLockingFailureException("lock not available"));

            // when & then
            assertThatThrownBy(() -> sut.consume(AUCTION_ID))
                    .isInstanceOf(HoldException.class)
                    .extracting(e -> ((HoldException) e).getErrorCode())
                    .isEqualTo(HoldErrorCode.LOCK_ACQUISITION_FAILED);

            verify(holdRepository, never()).delete(any());
        }
    }

}