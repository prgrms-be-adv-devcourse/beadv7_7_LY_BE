package site.coreservice.settlement.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("SettlementBatch")
class SettlementBatchTest {

    private static final Long SELLER_ID = 302L;
    private static final Money TOTAL_AMOUNT = Money.of(100_000);

    @Nested
    @DisplayName("생성")
    class Creation {

        @Test
        @DisplayName("생성 시 전달한 값들이 그대로 저장된다")
        void createStoresGivenValues() {
            // given
            LocalDateTime periodFrom = LocalDateTime.now().minusDays(7);
            LocalDateTime periodTo = LocalDateTime.now();
            LocalDateTime confirmedAt = LocalDateTime.now();

            // when
            SettlementBatch batch = SettlementBatch.of(SELLER_ID, TOTAL_AMOUNT, periodFrom, periodTo, confirmedAt);

            // then
            assertThat(batch.getSellerId()).isEqualTo(SELLER_ID);
            assertThat(batch.getTotalAmount()).isEqualTo(TOTAL_AMOUNT);
            assertThat(batch.getPeriodFrom()).isEqualTo(periodFrom);
            assertThat(batch.getPeriodTo()).isEqualTo(periodTo);
            assertThat(batch.getConfirmedAt()).isEqualTo(confirmedAt);
        }

        @Test
        @DisplayName("periodFrom이 periodTo보다 늦으면 예외가 발생한다")
        void periodFromAfterPeriodTo_throwsException() {
            // given
            LocalDateTime periodFrom = LocalDateTime.now();
            LocalDateTime periodTo = periodFrom.minusDays(1);

            // when & then
            assertThatThrownBy(() -> SettlementBatch.of(SELLER_ID, TOTAL_AMOUNT, periodFrom, periodTo, LocalDateTime.now()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("periodFrom은 periodTo보다 이전이어야 합니다.");
        }

        @Test
        @DisplayName("periodFrom과 periodTo가 같으면 예외가 발생한다")
        void periodFromEqualsPeriodTo_throwsException() {
            // given
            LocalDateTime period = LocalDateTime.now();

            // when & then
            assertThatThrownBy(() -> SettlementBatch.of(SELLER_ID, TOTAL_AMOUNT, period, period, LocalDateTime.now()))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
