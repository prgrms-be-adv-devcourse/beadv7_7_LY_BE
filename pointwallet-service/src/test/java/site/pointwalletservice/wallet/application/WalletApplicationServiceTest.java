package site.pointwalletservice.wallet.application;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import site.pointwalletservice.shared.Money;
import site.pointwalletservice.wallet.domain.InsufficientBalanceException;
import site.pointwalletservice.wallet.domain.Wallet;
import site.pointwalletservice.wallet.domain.WalletRepository;
import site.pointwalletservice.wallet.exception.WalletNotFoundException;


@ExtendWith(MockitoExtension.class)
@DisplayName("WalletApplicationService")
class WalletApplicationServiceTest {

    @Mock
    private WalletRepository walletRepository;

    private WalletApplicationService sut;

    private static final Long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        sut = new WalletApplicationService(walletRepository);
    }

    private Wallet walletWithBalance(Money balance) {
        Wallet wallet = Wallet.open(USER_ID);
        ReflectionTestUtils.setField(wallet, "id", 100L);
        wallet.charge(balance);
        return wallet;
    }

    @Nested
    @DisplayName("충전 (charge)")
    class Charge {

        @Test
        @DisplayName("기존 지갑이 있으면 그 지갑에 충전한다")
        void charge_기존지갑이_있으면_그대로_충전() {
            // given
            Wallet wallet = walletWithBalance(Money.of(5_000));
            when(walletRepository.findByUserIdForUpdate(USER_ID)).thenReturn(Optional.of(wallet));

            // when
            WalletBalanceResult result = sut.charge(USER_ID, Money.of(10_000));

            // then
            assertThat(result.walletId()).isEqualTo(100L);
            assertThat(result.balanceAfter()).isEqualTo(Money.of(15_000));
            verify(walletRepository).save(wallet);
        }

        @Test
        @DisplayName("지갑이 없으면 새로 개설한 뒤 충전한다")
        void charge_지갑이_없으면_새로_개설() {
            // given
            when(walletRepository.findByUserIdForUpdate(USER_ID)).thenReturn(Optional.empty());
            when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // when
            WalletBalanceResult result = sut.charge(USER_ID, Money.of(10_000));

            // then
            assertThat(result.balanceAfter()).isEqualTo(Money.of(10_000));
            // 개설 시 1번 + 충전 반영 후 1번
            verify(walletRepository, times(2)).save(any(Wallet.class));
        }
    }

    @Nested
    @DisplayName("환입 (credit)")
    class Credit {

        @Test
        @DisplayName("기존 지갑이 있으면 그 지갑에 환입한다")
        void credit_기존지갑이_있으면_환입된다() {
            // given
            Wallet wallet = walletWithBalance(Money.of(5_000));
            when(walletRepository.findByUserIdForUpdate(USER_ID)).thenReturn(Optional.of(wallet));

            // when
            WalletBalanceResult result = sut.credit(USER_ID, Money.of(10_000));

            // then
            assertThat(result.walletId()).isEqualTo(100L);
            assertThat(result.balanceAfter()).isEqualTo(Money.of(15_000));
            verify(walletRepository).save(wallet);
        }

        @Test
        @DisplayName("지갑이 없으면 charge()와 달리 자동 개설하지 않고 WalletNotFoundException을 던진다")
        void credit_지갑이_없으면_자동개설하지_않고_예외() {
            // given
            when(walletRepository.findByUserIdForUpdate(USER_ID)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> sut.credit(USER_ID, Money.of(10_000)))
                    .isInstanceOf(WalletNotFoundException.class);

            verify(walletRepository, never()).save(any(Wallet.class));
        }
    }

    @Nested
    @DisplayName("차감 (deduct)")
    class Deduct {

        @Test
        @DisplayName("잔액이 충분하면 차감하고 결과를 반환한다")
        void deduct_잔액이_충분하면_차감된다() {
            // given
            Wallet wallet = walletWithBalance(Money.of(10_000));
            when(walletRepository.findByUserIdForUpdate(USER_ID)).thenReturn(Optional.of(wallet));

            // when
            WalletBalanceResult result = sut.deduct(USER_ID, Money.of(4_000));

            // then
            assertThat(result.walletId()).isEqualTo(100L);
            assertThat(result.balanceAfter()).isEqualTo(Money.of(6_000));
            verify(walletRepository).save(wallet);
        }

        @Test
        @DisplayName("지갑이 없으면 WalletNotFoundException을 던진다")
        void deduct_지갑이_없으면_예외() {
            // given
            when(walletRepository.findByUserIdForUpdate(USER_ID)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> sut.deduct(USER_ID, Money.of(1_000)))
                    .isInstanceOf(WalletNotFoundException.class);

            verify(walletRepository, never()).save(any(Wallet.class));
        }

        @Test
        @DisplayName("잔액이 부족하면 Wallet이 던진 InsufficientBalanceException을 그대로 전파한다")
        void deduct_잔액부족이면_InsufficientBalanceException_전파() {
            // given
            Wallet wallet = walletWithBalance(Money.of(1_000));
            when(walletRepository.findByUserIdForUpdate(USER_ID)).thenReturn(Optional.of(wallet));

            // when & then
            assertThatThrownBy(() -> sut.deduct(USER_ID, Money.of(10_000)))
                    .isInstanceOf(InsufficientBalanceException.class);

            verify(walletRepository, never()).save(any(Wallet.class));
        }
    }

    @Nested
    @DisplayName("잔액 조회 (getBalance)")
    class GetBalance {

        @Test
        @DisplayName("지갑이 있으면 현재 잔액을 반환한다")
        void getBalance_지갑이_있으면_잔액_반환() {
            // given
            Wallet wallet = walletWithBalance(Money.of(7_000));
            when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.of(wallet));

            // when
            Money balance = sut.getBalance(USER_ID);

            // then
            assertThat(balance).isEqualTo(Money.of(7_000));
        }

        @Test
        @DisplayName("지갑이 없으면 예외 없이 0원을 반환한다")
        void getBalance_지갑이_없으면_0원() {
            // given
            when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

            // when
            Money balance = sut.getBalance(USER_ID);

            // then
            assertThat(balance).isEqualTo(Money.zero());
        }
    }

    @Nested
    @DisplayName("지갑 사전 락 (lockForUpdate)")
    class LockForUpdate {

        @Test
        @DisplayName("단일 인자 - 해당 유저 지갑에 락 조회를 1번 건다")
        void lockForUpdate_단일인자_해당유저만_락조회() {
            // given
            when(walletRepository.findByUserIdForUpdate(USER_ID)).thenReturn(Optional.empty());

            // when
            sut.lockForUpdate(USER_ID);

            // then
            verify(walletRepository, times(1)).findByUserIdForUpdate(USER_ID);
        }

        @Test
        @DisplayName("두 번째 유저가 null이면 첫 번째 유저만 락을 건다")
        void lockForUpdate_두번째유저가_null이면_하나만_잠근다() {
            // given
            when(walletRepository.findByUserIdForUpdate(USER_ID)).thenReturn(Optional.empty());

            // when
            sut.lockForUpdate(USER_ID, null);

            // then
            verify(walletRepository, times(1)).findByUserIdForUpdate(USER_ID);
            verify(walletRepository, times(1)).findByUserIdForUpdate(any());
        }

        @Test
        @DisplayName("두 유저가 동일하면 중복으로 잠그지 않고 한 번만 잠근다")
        void lockForUpdate_두유저가_같으면_한번만_잠근다() {
            // given
            when(walletRepository.findByUserIdForUpdate(USER_ID)).thenReturn(Optional.empty());

            // when
            sut.lockForUpdate(USER_ID, USER_ID);

            // then
            verify(walletRepository, times(1)).findByUserIdForUpdate(USER_ID);
            verify(walletRepository, times(1)).findByUserIdForUpdate(any());
        }

        @Test
        @DisplayName("두 유저가 다르면 데드락 방지를 위해 작은 ID를 먼저 잠근다 - 인자 순서(큰→작은)로 넣어도 동일하다")
        void lockForUpdate_서로다른유저면_작은ID부터_순서대로_잠근다() {
            // given
            Long smallerId = 100L;
            Long largerId = 200L;
            when(walletRepository.findByUserIdForUpdate(smallerId)).thenReturn(Optional.empty());
            when(walletRepository.findByUserIdForUpdate(largerId)).thenReturn(Optional.empty());

            // when: 일부러 큰 ID를 첫 인자로 넘겨도
            sut.lockForUpdate(largerId, smallerId);

            // then: 실제 잠그는 순서는 작은 ID가 먼저여야 한다
            var inOrder = org.mockito.Mockito.inOrder(walletRepository);
            inOrder.verify(walletRepository).findByUserIdForUpdate(smallerId);
            inOrder.verify(walletRepository).findByUserIdForUpdate(largerId);
        }
    }
}