package site.coreservice.product.infrastructure.client.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import site.coreservice.product.domain.price.ClosedAuction;
import site.coreservice.product.domain.price.MediaCondition;
import site.coreservice.product.exception.AuctionContractViolationException;

class AuctionSummaryPayloadTest {

    private static final LocalDateTime END_AT = LocalDateTime.of(2026, 7, 27, 20, 31);

    private AuctionSummaryPayload payload(String itemCondition, long bidCount, BigDecimal finalPrice, String status) {
        return new AuctionSummaryPayload(7L, 3L, itemCondition, bidCount, finalPrice, END_AT, status);
    }

    @Test
    @DisplayName("경매 응답 필드를 ClosedAuction의 이름과 타입으로 옮긴다")
    void toClosedAuction_매핑_4건() {
        // given
        AuctionSummaryPayload payload = payload("NEAR_MINT", 3L, new BigDecimal("15000"), "ENDED_WON");

        // when
        ClosedAuction closedAuction = payload.toClosedAuction();

        // then: itemCondition→mediaCondition, BigDecimal→Long, long→Integer, endAt→closedAt
        assertThat(closedAuction.mediaCondition()).isEqualTo(MediaCondition.NEAR_MINT);
        assertThat(closedAuction.finalPrice()).isEqualTo(15_000L);
        assertThat(closedAuction.bidCount()).isEqualTo(3);
        assertThat(closedAuction.closedAt()).isEqualTo(END_AT);
        assertThat(closedAuction.auctionId()).isEqualTo(7L);
        assertThat(closedAuction.productId()).isEqualTo(3L);
        assertThat(closedAuction.status()).isEqualTo("ENDED_WON");
    }

    @Test
    @DisplayName("소수부가 0인 금액은 그대로 통과한다")
    void toClosedAuction_소수점표기_통과() {
        // given: DB가 decimal(19,2)라 정상 금액도 15000.00으로 올 수 있다
        AuctionSummaryPayload payload = payload("MINT", 1L, new BigDecimal("15000.00"), "ENDED_WON");

        // when
        ClosedAuction closedAuction = payload.toClosedAuction();

        // then
        assertThat(closedAuction.finalPrice()).isEqualTo(15_000L);
    }

    @Test
    @DisplayName("낙찰가에 소수부가 있으면 조용히 자르지 않고 계약 위반으로 올린다")
    void toClosedAuction_소수부_예외() {
        // given
        AuctionSummaryPayload payload = payload("MINT", 1L, new BigDecimal("15000.75"), "ENDED_WON");

        // when-then: longValue()였다면 15000으로 조용히 잘렸을 값
        assertThatThrownBy(payload::toClosedAuction)
                .isInstanceOf(AuctionContractViolationException.class)
                .hasMessageContaining("15000.75");
    }

    @Test
    @DisplayName("모르는 컨디션 등급은 계약 위반으로 올린다")
    void toClosedAuction_모르는_컨디션_예외() {
        // given
        AuctionSummaryPayload payload = payload("SUPER_MINT", 1L, new BigDecimal("15000"), "ENDED_WON");

        // when-then
        assertThatThrownBy(payload::toClosedAuction)
                .isInstanceOf(AuctionContractViolationException.class)
                .hasMessageContaining("SUPER_MINT");
    }

    @Test
    @DisplayName("컨디션이 비어 있어도 계약 위반으로 올린다")
    void toClosedAuction_컨디션_없음_예외() {
        // given
        AuctionSummaryPayload payload = payload(null, 1L, new BigDecimal("15000"), "ENDED_WON");

        // when-then
        assertThatThrownBy(payload::toClosedAuction)
                .isInstanceOf(AuctionContractViolationException.class);
    }

    @Test
    @DisplayName("낙찰가가 비어 있으면 null 그대로 넘겨 뒤쪽 검증에 맡긴다")
    void toClosedAuction_낙찰가_null_통과() {
        // given: 경매는 입찰이 없으면 finalPrice를 null로 준다
        AuctionSummaryPayload payload = payload("MINT", 0L, null, "ENDED_FAILED");

        // when
        ClosedAuction closedAuction = payload.toClosedAuction();

        // then: PriceHistory.of가 "낙찰가는 1 이상"으로 거른다
        assertThat(closedAuction.finalPrice()).isNull();
    }

    @Test
    @DisplayName("입찰 수가 int 범위를 넘으면 계약 위반으로 올린다")
    void toClosedAuction_입찰수_범위초과_예외() {
        // given
        AuctionSummaryPayload payload = payload("MINT", Long.MAX_VALUE, new BigDecimal("15000"), "ENDED_WON");

        // when-then
        assertThatThrownBy(payload::toClosedAuction)
                .isInstanceOf(AuctionContractViolationException.class);
    }
}
