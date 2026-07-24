package site.coreservice.pointwallet.ledger.infrastructure;
import static org.assertj.core.api.Assertions.assertThat;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;
import site.coreservice.pointwallet.ledger.domain.PointTransaction;
import site.coreservice.pointwallet.ledger.domain.PointTransactionRepository;
import site.coreservice.pointwallet.ledger.domain.PointTransactionSearchPage;
import site.coreservice.pointwallet.ledger.domain.PointTransactionType;
import site.coreservice.pointwallet.shared.Money;
import site.coreservice.support.RepositoryTest;

@RepositoryTest
@Import(PointTransactionRepositoryImpl.class)
class PointTransactionRepositoryImplTest {

    @Autowired
    private PointTransactionRepository pointTransactionRepository;

    @Autowired
    private PointTransactionJpaRepository pointTransactionJpaRepository;

    private static final Long WALLET_ID = 100L;

    // occurredAt은 updatable=false라 저장 후엔 못 바꾸지만, 최초 save(=insert) 전에 필드를 세팅해두면
    // 그대로 반영된다 — 시간 관련 테스트를 결정론적으로 만들기 위한 용도.
    private PointTransaction saveTransaction(Long walletId, PointTransactionType type, long amount, LocalDateTime occurredAt) {
        PointTransaction transaction = PointTransaction.record(walletId, type, Money.of(amount), Money.of(amount), 1L);
        ReflectionTestUtils.setField(transaction, "occurredAt", occurredAt);
        return pointTransactionJpaRepository.save(transaction);
    }

    @Test
    @DisplayName("walletId로 조회하면 다른 지갑의 거래는 제외하고, 최신순(occurredAt desc)으로 반환한다")
    void search_walletId만() {
        // given
        LocalDateTime now = LocalDateTime.now();
        saveTransaction(WALLET_ID, PointTransactionType.DEPOSIT, 10_000, now.minusMinutes(1));
        saveTransaction(WALLET_ID, PointTransactionType.HOLD, 3_000, now);
        saveTransaction(999L, PointTransactionType.DEPOSIT, 5_000, now); // 다른 지갑 - 제외돼야 함

        // when
        PointTransactionSearchPage result = pointTransactionRepository.search(WALLET_ID, null, null, null, 0, 20);

        // then
        assertThat(result.totalElements()).isEqualTo(2L);
        assertThat(result.content()).extracting(PointTransaction::getType)
                .containsExactly(PointTransactionType.HOLD, PointTransactionType.DEPOSIT);
    }

    @Test
    @DisplayName("type으로 필터링한다")
    void search_type_필터() {
        // given
        LocalDateTime now = LocalDateTime.now();
        saveTransaction(WALLET_ID, PointTransactionType.DEPOSIT, 10_000, now);
        saveTransaction(WALLET_ID, PointTransactionType.HOLD, 3_000, now);

        // when
        PointTransactionSearchPage result =
                pointTransactionRepository.search(WALLET_ID, PointTransactionType.HOLD, null, null, 0, 20);

        // then
        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).getType()).isEqualTo(PointTransactionType.HOLD);
    }

    @Test
    @DisplayName("from~to 범위를 벗어난 거래는 결과에서 제외한다")
    void search_기간_필터() {
        // given
        LocalDateTime now = LocalDateTime.now();
        saveTransaction(WALLET_ID, PointTransactionType.DEPOSIT, 10_000, now);
        LocalDateTime past = now.minusDays(1);
        LocalDateTime future = now.plusDays(1);

        // when
        PointTransactionSearchPage inRange = pointTransactionRepository.search(WALLET_ID, null, past, future, 0, 20);
        PointTransactionSearchPage tooLate = pointTransactionRepository.search(WALLET_ID, null, future, null, 0, 20);

        // then
        assertThat(inRange.content()).hasSize(1);
        assertThat(tooLate.content()).isEmpty();
    }

    @Test
    @DisplayName("페이징이 되고 totalElements는 조건에 맞는 전체 개수와 일치한다")
    void search_페이징() {
        // given
        LocalDateTime now = LocalDateTime.now();
        saveTransaction(WALLET_ID, PointTransactionType.DEPOSIT, 1_000, now.minusMinutes(2));
        saveTransaction(WALLET_ID, PointTransactionType.DEPOSIT, 2_000, now.minusMinutes(1));
        saveTransaction(WALLET_ID, PointTransactionType.DEPOSIT, 3_000, now);

        // when
        PointTransactionSearchPage firstPage = pointTransactionRepository.search(WALLET_ID, null, null, null, 0, 2);
        PointTransactionSearchPage secondPage = pointTransactionRepository.search(WALLET_ID, null, null, null, 1, 2);

        // then
        assertThat(firstPage.content()).hasSize(2);
        assertThat(secondPage.content()).hasSize(1);
        assertThat(firstPage.totalElements()).isEqualTo(3L);
    }
}