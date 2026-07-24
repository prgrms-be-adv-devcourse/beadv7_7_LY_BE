package site.coreservice.pointwallet.ledger.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import site.coreservice.pointwallet.ledger.domain.PointTransaction;
import site.coreservice.pointwallet.ledger.domain.PointTransactionRepository;
import site.coreservice.pointwallet.ledger.domain.PointTransactionType;
import site.coreservice.pointwallet.shared.Money;

@ExtendWith(MockitoExtension.class)
@DisplayName("PointTransactionApplicationService")
class PointTransactionApplicationServiceTest {

    @Mock
    private PointTransactionRepository pointTransactionRepository;

    private PointTransactionApplicationService sut;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        sut = new PointTransactionApplicationService(pointTransactionRepository);
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
}