package site.fulfillmentservice.settlement.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import site.fulfillmentservice.settlement.domain.Money;
import site.fulfillmentservice.settlement.domain.SettlementItem;
import site.fulfillmentservice.settlement.domain.SettlementItemRepository;
import site.fulfillmentservice.settlement.domain.SettlementStatus;
import site.fulfillmentservice.support.RepositoryTest;

@RepositoryTest
@Import(SettlementItemRepositoryImpl.class)
class SettlementItemRepositoryImplTest {

    @Autowired
    private SettlementItemRepository settlementItemRepository;

    @Autowired
    private SettlementItemJpaRepository settlementItemJpaRepository;

    private static final Long SELLER_ID = 302L;

    private SettlementItem settlementItem(Long orderId) {
        return SettlementItem.of(orderId, SELLER_ID, Money.of(85_000), BigDecimal.valueOf(0.1000), LocalDateTime.now());
    }

    private SettlementItem settlementItem(Long orderId, LocalDateTime completedAt) {
        return SettlementItem.of(orderId, SELLER_ID, Money.of(85_000), BigDecimal.valueOf(0.1000), completedAt);
    }

    @Test
    void findByOrderId는_저장된_정산항목을_반환한다() {
        settlementItemJpaRepository.save(settlementItem(5001L));

        Optional<SettlementItem> result = settlementItemRepository.findByOrderId(5001L);

        assertThat(result).isPresent();
        assertThat(result.get().getSellerId()).isEqualTo(SELLER_ID);
    }

    @Test
    void findByOrderId_없으면_빈값() {
        Optional<SettlementItem> result = settlementItemRepository.findByOrderId(9999L);

        assertThat(result).isEmpty();
    }

    @Test
    void existsByOrderId는_존재_여부를_반환한다() {
        settlementItemJpaRepository.save(settlementItem(5001L));

        assertThat(settlementItemRepository.existsByOrderId(5001L)).isTrue();
        assertThat(settlementItemRepository.existsByOrderId(9999L)).isFalse();
    }

    @Test
    void 같은_orderId로_두번째_정산항목을_저장하면_유니크_제약으로_실패한다() {
        settlementItemJpaRepository.saveAndFlush(settlementItem(5001L));

        assertThatThrownBy(() -> settlementItemJpaRepository.saveAndFlush(settlementItem(5001L)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void findDistinctSellerIdsByStatusAndCompletedAtBefore는_PENDING이고_기준_이전인_항목의_판매자만_중복없이_반환한다() {
        LocalDateTime now = LocalDateTime.now();
        settlementItemJpaRepository.save(settlementItem(5001L, now.minusDays(1)));
        settlementItemJpaRepository.save(settlementItem(5002L, now.minusDays(1)));
        settlementItemJpaRepository.save(settlementItem(5003L, now.plusDays(1)));

        List<Long> result = settlementItemRepository.findDistinctSellerIdsByStatusAndCompletedAtBefore(SettlementStatus.PENDING, now);

        assertThat(result).containsExactly(SELLER_ID);
    }

    @Test
    void findDistinctSellerIdsByStatusAndCompletedAtBefore는_CONFIRMED_상태는_제외한다() {
        LocalDateTime now = LocalDateTime.now();
        SettlementItem confirmedItem = settlementItemJpaRepository.save(settlementItem(5001L, now.minusDays(1)));
        confirmedItem.markConfirmed(9001L, now);
        settlementItemJpaRepository.save(confirmedItem);

        List<Long> result = settlementItemRepository.findDistinctSellerIdsByStatusAndCompletedAtBefore(SettlementStatus.PENDING, now);

        assertThat(result).isEmpty();
    }
}
