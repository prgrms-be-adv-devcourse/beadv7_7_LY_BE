package site.coreservice.settlement.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import site.coreservice.settlement.domain.Money;
import site.coreservice.settlement.domain.SettlementBatch;
import site.coreservice.settlement.domain.SettlementBatchRepository;
import site.coreservice.support.RepositoryTest;

@RepositoryTest
@Import(SettlementBatchRepositoryImpl.class)
class SettlementBatchRepositoryImplTest {

    @Autowired
    private SettlementBatchRepository settlementBatchRepository;

    @Autowired
    private SettlementBatchJpaRepository settlementBatchJpaRepository;

    private static final Long SELLER_ID = 302L;

    // DB 컬럼은 초 단위까지만 저장되므로, 나노초까지 있는 값으로 exists 쿼리를 비교하면 항상 불일치한다.
    private final LocalDateTime periodFrom = LocalDateTime.now().minusDays(7).truncatedTo(ChronoUnit.SECONDS);
    private final LocalDateTime periodTo = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);

    private SettlementBatch settlementBatch(Long sellerId, LocalDateTime periodFrom, LocalDateTime periodTo) {
        return SettlementBatch.of(sellerId, Money.of(100_000), periodFrom, periodTo, LocalDateTime.now());
    }

    @Test
    void existsBySellerIdAndPeriodFromAndPeriodTo는_저장된_배치가_있으면_true를_반환한다() {
        settlementBatchJpaRepository.save(settlementBatch(SELLER_ID, periodFrom, periodTo));

        boolean exists = settlementBatchRepository.existsBySellerIdAndPeriodFromAndPeriodTo(SELLER_ID, periodFrom, periodTo);

        assertThat(exists).isTrue();
    }

    @Test
    void existsBySellerIdAndPeriodFromAndPeriodTo_없으면_false를_반환한다() {
        boolean exists = settlementBatchRepository.existsBySellerIdAndPeriodFromAndPeriodTo(SELLER_ID, periodFrom, periodTo);

        assertThat(exists).isFalse();
    }

    @Test
    void 같은_판매자_같은_기간으로_두번째_배치를_저장하면_유니크_제약으로_실패한다() {
        settlementBatchJpaRepository.saveAndFlush(settlementBatch(SELLER_ID, periodFrom, periodTo));

        assertThatThrownBy(() -> settlementBatchJpaRepository.saveAndFlush(settlementBatch(SELLER_ID, periodFrom, periodTo)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void 같은_판매자여도_기간이_다르면_저장할_수_있다() {
        settlementBatchJpaRepository.saveAndFlush(settlementBatch(SELLER_ID, periodFrom, periodTo));

        SettlementBatch saved = settlementBatchJpaRepository.saveAndFlush(
                settlementBatch(SELLER_ID, periodTo, periodTo.plusDays(7)));

        assertThat(saved.getId()).isNotNull();
    }
}
