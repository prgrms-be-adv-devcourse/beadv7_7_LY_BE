package site.coreservice.pointwallet.ledger.application;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import site.coreservice.pointwallet.ledger.application.dto.PointTransactionSearchResult;
import site.coreservice.pointwallet.ledger.domain.PointTransaction;
import site.coreservice.pointwallet.ledger.domain.PointTransactionRepository;
import site.coreservice.pointwallet.ledger.domain.PointTransactionSearchPage;
import site.coreservice.pointwallet.ledger.domain.PointTransactionType;
import site.coreservice.pointwallet.ledger.exception.LedgerErrorCode;
import site.coreservice.pointwallet.ledger.exception.LedgerException;
import site.coreservice.pointwallet.shared.Money;
import site.coreservice.pointwallet.wallet.application.WalletService;

@ExtendWith(MockitoExtension.class)
@DisplayName("PointTransactionApplicationService")
class PointTransactionApplicationServiceTest {

    @Mock
    private PointTransactionRepository pointTransactionRepository;

    @Mock
    private WalletService walletService;

    private PointTransactionApplicationService sut;

    private static final Long USER_ID = 1L;
    private static final Long WALLET_ID = 100L;

    @BeforeEach
    void setUp() {
        sut = new PointTransactionApplicationService(pointTransactionRepository, walletService);
    }

    @Test
    @DisplayName("record를 호출하면 사실 1건을 만들어 저장한다")
    void record_사실_1건을_만들어_저장한다() {
        // when
        sut.record(100L, PointTransactionType.DEPOSIT, Money.of(10_000), Money.of(10_000), 1L);

        // then
        ArgumentCaptor<PointTransaction> captor = ArgumentCaptor.forClass(PointTransaction.class);
        verify(pointTransactionRepository).save(captor.capture());

        PointTransaction saved = captor.getValue();
        assertThat(saved.getWalletId()).isEqualTo(100L);
        assertThat(saved.getType()).isEqualTo(PointTransactionType.DEPOSIT);
        assertThat(saved.getAmount()).isEqualTo(Money.of(10_000));
        assertThat(saved.getBalanceAfter()).isEqualTo(Money.of(10_000));
        assertThat(saved.getRelatedId()).isEqualTo(1L);
    }

    @Nested
    @DisplayName("거래내역 조회 (findTransactions)")
    class FindTransactions {

        @Test
        @DisplayName("지갑이 없으면 빈 페이지를 반환하고 저장소는 조회하지 않는다")
        void 지갑이_없으면_빈페이지() {
            // given
            when(walletService.findWalletId(USER_ID)).thenReturn(Optional.empty());

            // when
            PointTransactionSearchResult result = sut.findTransactions(USER_ID, null, null, null, 0, 20);

            // then
            assertThat(result.content()).isEmpty();
            assertThat(result.totalElements()).isZero();
            verify(pointTransactionRepository, never()).search(any(), any(), any(), any(), anyIntArg(), anyIntArg());
        }

        @Test
        @DisplayName("정상 흐름이면 walletId로 저장소를 조회해 결과를 조립한다")
        void 정상흐름() {
            // given
            when(walletService.findWalletId(USER_ID)).thenReturn(Optional.of(WALLET_ID));
            PointTransaction transaction =
                    PointTransaction.record(WALLET_ID, PointTransactionType.DEPOSIT, Money.of(10_000), Money.of(10_000), 1L);
            when(pointTransactionRepository.search(WALLET_ID, null, null, null, 0, 20))
                    .thenReturn(new PointTransactionSearchPage(List.of(transaction), 1L));

            // when
            PointTransactionSearchResult result = sut.findTransactions(USER_ID, null, null, null, 0, 20);

            // then
            assertThat(result.content()).hasSize(1);
            assertThat(result.totalElements()).isEqualTo(1L);
            assertThat(result.page()).isZero();
            assertThat(result.size()).isEqualTo(20);
        }

        @Test
        @DisplayName("type이 유효하지 않으면 LedgerException(INVALID_TRANSACTION_TYPE)을 던지고 저장소를 조회하지 않는다")
        void type이_유효하지않으면_예외() {
            // when & then
            assertThatThrownBy(() -> sut.findTransactions(USER_ID, "NOT_A_TYPE", null, null, 0, 20))
                    .isInstanceOf(LedgerException.class)
                    .extracting(e -> ((LedgerException) e).getErrorCode())
                    .isEqualTo(LedgerErrorCode.INVALID_TRANSACTION_TYPE);

            verify(walletService, never()).findWalletId(any());
            verify(pointTransactionRepository, never()).search(any(), any(), any(), any(), anyIntArg(), anyIntArg());
        }

        @Test
        @DisplayName("type이 비어있으면 조건 없이(null) 저장소를 조회한다")
        void type이_비어있으면_조건없이_조회() {
            // given
            when(walletService.findWalletId(USER_ID)).thenReturn(Optional.of(WALLET_ID));
            when(pointTransactionRepository.search(eq(WALLET_ID), isNull(), any(), any(), anyIntArg(), anyIntArg()))
                    .thenReturn(new PointTransactionSearchPage(List.of(), 0L));

            // when
            sut.findTransactions(USER_ID, "  ", null, null, 0, 20);

            // then
            verify(pointTransactionRepository).search(eq(WALLET_ID), isNull(), any(), any(), anyIntArg(), anyIntArg());
        }

        @Test
        @DisplayName("page는 음수면 0으로, size는 1 미만이면 20, 100 초과면 100으로 보정한다")
        void page_size_보정() {
            // given
            when(walletService.findWalletId(USER_ID)).thenReturn(Optional.of(WALLET_ID));
            when(pointTransactionRepository.search(any(), any(), any(), any(), anyIntArg(), anyIntArg()))
                    .thenReturn(new PointTransactionSearchPage(List.of(), 0L));

            // when
            PointTransactionSearchResult negativePage = sut.findTransactions(USER_ID, null, null, null, -5, 20);
            PointTransactionSearchResult tooSmallSize = sut.findTransactions(USER_ID, null, null, null, 0, 0);
            PointTransactionSearchResult tooBigSize = sut.findTransactions(USER_ID, null, null, null, 0, 1000);

            // then
            assertThat(negativePage.page()).isZero();
            assertThat(tooSmallSize.size()).isEqualTo(20);
            assertThat(tooBigSize.size()).isEqualTo(100);
        }

        private int anyIntArg() {
            return org.mockito.ArgumentMatchers.anyInt();
        }
    }
}