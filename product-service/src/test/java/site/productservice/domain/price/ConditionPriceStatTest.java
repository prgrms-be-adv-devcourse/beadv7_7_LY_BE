package site.productservice.domain.price;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ConditionPriceStatTest {

    private static final LocalDateTime CLOSED_AT = LocalDateTime.of(2026, 7, 10, 20, 31);
    private static final LocalDateTime CONFIRMED_AT = LocalDateTime.of(2026, 7, 11, 10, 0);

    private PriceHistory trade(long auctionId, MediaCondition condition, long finalPrice) {
        return PriceHistory.of(
                new ClosedAuction(auctionId, 55L, condition, finalPrice, 5, CLOSED_AT, "ENDED_WON"),
                CONFIRMED_AT);
    }

    @Test
    @DisplayName("거래 목록을 컨디션별로 묶어 건수·평균·최저·최고를 계산한다")
    void listFrom_컨디션별로_묶어_통계를_계산() {
        // given
        List<PriceHistory> trades = List.of(
                trade(1L, MediaCondition.NEAR_MINT, 80000L),
                trade(2L, MediaCondition.NEAR_MINT, 90000L),
                trade(3L, MediaCondition.GOOD, 20000L));

        // when
        List<ConditionPriceStat> stats = ConditionPriceStat.listFrom(trades);

        // then
        assertThat(stats).hasSize(2);
        ConditionPriceStat nearMint = stats.get(0);
        assertThat(nearMint.condition()).isEqualTo(MediaCondition.NEAR_MINT);
        assertThat(nearMint.sampleCount()).isEqualTo(2L);
        assertThat(nearMint.averagePrice()).isEqualTo(85000.0);
        assertThat(nearMint.lowestPrice()).isEqualTo(80000L);
        assertThat(nearMint.highestPrice()).isEqualTo(90000L);
        ConditionPriceStat good = stats.get(1);
        assertThat(good.condition()).isEqualTo(MediaCondition.GOOD);
        assertThat(good.sampleCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("입력 순서와 무관하게 컨디션 enum 선언 순으로 정렬해 반환한다")
    void listFrom_enum_선언_순_정렬() {
        // given — 선언 역순(POOR 먼저)으로 입력
        List<PriceHistory> trades = List.of(
                trade(1L, MediaCondition.POOR, 5000L),
                trade(2L, MediaCondition.GOOD, 20000L),
                trade(3L, MediaCondition.MINT, 120000L));

        // when
        List<ConditionPriceStat> stats = ConditionPriceStat.listFrom(trades);

        // then — MINT → GOOD → POOR (알파벳순이면 GOOD이 첫 번째가 되므로 구분됨)
        assertThat(stats).extracting(ConditionPriceStat::condition)
                .containsExactly(MediaCondition.MINT, MediaCondition.GOOD, MediaCondition.POOR);
    }

    @Test
    @DisplayName("한 건짜리 컨디션은 평균·최저·최고가 모두 그 값이다")
    void listFrom_한_건이면_평균_최저_최고_동일() {
        // given
        List<PriceHistory> trades = List.of(trade(1L, MediaCondition.VERY_GOOD, 33000L));

        // when
        List<ConditionPriceStat> stats = ConditionPriceStat.listFrom(trades);

        // then
        assertThat(stats.get(0).averagePrice()).isEqualTo(33000.0);
        assertThat(stats.get(0).lowestPrice()).isEqualTo(33000L);
        assertThat(stats.get(0).highestPrice()).isEqualTo(33000L);
    }

    @Test
    @DisplayName("평균은 소수를 반올림하지 않고 그대로 보존한다 (표현 정책은 응답 변환의 몫)")
    void listFrom_평균_소수_보존() {
        // given
        List<PriceHistory> trades = List.of(
                trade(1L, MediaCondition.NEAR_MINT, 100L),
                trade(2L, MediaCondition.NEAR_MINT, 101L));

        // when
        List<ConditionPriceStat> stats = ConditionPriceStat.listFrom(trades);

        // then
        assertThat(stats.get(0).averagePrice()).isEqualTo(100.5);
    }

    @Test
    @DisplayName("빈 목록이면 빈 리스트를 반환한다 (예외 아님)")
    void listFrom_빈_목록이면_빈_리스트() {
        // given-when
        List<ConditionPriceStat> stats = ConditionPriceStat.listFrom(List.of());

        // then
        assertThat(stats).isEmpty();
    }
}
