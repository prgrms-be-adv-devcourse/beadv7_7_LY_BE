package site.coreservice.settlement.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("CommissionPolicy")
class CommissionPolicyTest {

    private static final BigDecimal RATE = BigDecimal.valueOf(0.1000);

    @Nested
    @DisplayName("생성")
    class Creation {

        @Test
        @DisplayName("effectiveTo 없이 생성할 수 있다 (현재 유효 정책)")
        void createWithoutEffectiveTo() {
            // given
            LocalDateTime effectiveFrom = LocalDateTime.now();

            // when
            CommissionPolicy policy = CommissionPolicy.of(RATE, effectiveFrom, null);

            // then
            assertThat(policy.getCommissionRate()).isEqualByComparingTo(RATE);
            assertThat(policy.getEffectiveFrom()).isEqualTo(effectiveFrom);
            assertThat(policy.getEffectiveTo()).isNull();
        }

        @Test
        @DisplayName("effectiveFrom이 effectiveTo보다 늦으면 예외가 발생한다")
        void effectiveFromAfterEffectiveTo_throwsException() {
            // given
            LocalDateTime effectiveFrom = LocalDateTime.now();
            LocalDateTime effectiveTo = effectiveFrom.minusDays(1);

            // when & then
            assertThatThrownBy(() -> CommissionPolicy.of(RATE, effectiveFrom, effectiveTo))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("effectiveFrom은 effectiveTo보다 이전이어야 합니다.");
        }

        @Test
        @DisplayName("commissionRate가 0이면 생성할 수 있다")
        void createWithZeroRate() {
            // given & when
            CommissionPolicy policy = CommissionPolicy.of(BigDecimal.ZERO, LocalDateTime.now(), null);

            // then
            assertThat(policy.getCommissionRate()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("commissionRate가 음수면 예외가 발생한다")
        void negativeRate_throwsException() {
            // given
            BigDecimal negativeRate = BigDecimal.valueOf(-0.1);

            // when & then
            assertThatThrownBy(() -> CommissionPolicy.of(negativeRate, LocalDateTime.now(), null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("commissionRate는 0 이상 1 미만이어야 합니다.");
        }

        @Test
        @DisplayName("commissionRate가 1이면 예외가 발생한다")
        void rateEqualToOne_throwsException() {
            // given & when & then
            assertThatThrownBy(() -> CommissionPolicy.of(BigDecimal.ONE, LocalDateTime.now(), null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("commissionRate는 0 이상 1 미만이어야 합니다.");
        }

        @Test
        @DisplayName("commissionRate가 1을 초과하면 예외가 발생한다")
        void rateGreaterThanOne_throwsException() {
            // given
            BigDecimal overRate = BigDecimal.valueOf(1.5);

            // when & then
            assertThatThrownBy(() -> CommissionPolicy.of(overRate, LocalDateTime.now(), null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("commissionRate는 0 이상 1 미만이어야 합니다.");
        }
    }

    @Nested
    @DisplayName("유효 시점 판단 (isEffectiveAt)")
    class IsEffectiveAt {

        @Test
        @DisplayName("effectiveTo가 없으면 effectiveFrom 이후 언제든 유효하다")
        void openEnded_alwaysEffectiveAfterStart() {
            // given
            LocalDateTime effectiveFrom = LocalDateTime.now().minusDays(1);
            CommissionPolicy policy = CommissionPolicy.of(RATE, effectiveFrom, null);

            // when & then
            assertThat(policy.isEffectiveAt(effectiveFrom.plusYears(10))).isTrue();
        }

        @Test
        @DisplayName("effectiveFrom 이전이면 유효하지 않다")
        void beforeEffectiveFrom_notEffective() {
            // given
            LocalDateTime effectiveFrom = LocalDateTime.now();
            CommissionPolicy policy = CommissionPolicy.of(RATE, effectiveFrom, null);

            // when & then
            assertThat(policy.isEffectiveAt(effectiveFrom.minusSeconds(1))).isFalse();
        }

        @Test
        @DisplayName("effectiveTo 이후면 유효하지 않다")
        void afterEffectiveTo_notEffective() {
            // given
            LocalDateTime effectiveFrom = LocalDateTime.now().minusDays(2);
            LocalDateTime effectiveTo = LocalDateTime.now().minusDays(1);
            CommissionPolicy policy = CommissionPolicy.of(RATE, effectiveFrom, effectiveTo);

            // when & then
            assertThat(policy.isEffectiveAt(effectiveTo.plusSeconds(1))).isFalse();
        }

        @Test
        @DisplayName("effectiveFrom과 effectiveTo 사이면 유효하다")
        void withinRange_effective() {
            // given
            LocalDateTime effectiveFrom = LocalDateTime.now().minusDays(2);
            LocalDateTime effectiveTo = LocalDateTime.now().plusDays(2);
            CommissionPolicy policy = CommissionPolicy.of(RATE, effectiveFrom, effectiveTo);

            // when & then
            assertThat(policy.isEffectiveAt(LocalDateTime.now())).isTrue();
        }
    }

    @Nested
    @DisplayName("종료 (close)")
    class Close {

        @Test
        @DisplayName("무기한 정책을 종료하면 effectiveTo가 채워진다")
        void closeOpenEndedPolicy() {
            // given
            LocalDateTime effectiveFrom = LocalDateTime.now().minusDays(1);
            CommissionPolicy policy = CommissionPolicy.of(RATE, effectiveFrom, null);
            LocalDateTime closedAt = LocalDateTime.now();

            // when
            policy.close(closedAt);

            // then
            assertThat(policy.getEffectiveTo()).isEqualTo(closedAt);
        }

        @Test
        @DisplayName("이미 종료된 정책을 다시 종료하려 하면 예외가 발생한다")
        void closeAlreadyClosedPolicy_throwsException() {
            // given
            LocalDateTime effectiveFrom = LocalDateTime.now().minusDays(2);
            LocalDateTime effectiveTo = LocalDateTime.now().minusDays(1);
            CommissionPolicy policy = CommissionPolicy.of(RATE, effectiveFrom, effectiveTo);

            // when & then
            assertThatThrownBy(() -> policy.close(LocalDateTime.now()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("이미 종료된 정책입니다.");
        }

        @Test
        @DisplayName("effectiveFrom보다 이전 시각으로 종료하려 하면 예외가 발생한다")
        void closeBeforeEffectiveFrom_throwsException() {
            // given
            LocalDateTime effectiveFrom = LocalDateTime.now();
            CommissionPolicy policy = CommissionPolicy.of(RATE, effectiveFrom, null);

            // when & then
            assertThatThrownBy(() -> policy.close(effectiveFrom.minusDays(1)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("effectiveTo는 effectiveFrom보다 이후여야 합니다.");
        }
    }
}
