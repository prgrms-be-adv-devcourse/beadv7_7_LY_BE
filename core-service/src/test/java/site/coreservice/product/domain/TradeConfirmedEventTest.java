package site.coreservice.product.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TradeConfirmedEventTest {

    @Test
    @DisplayName("경매 id와 확정시각을 담아 생성되고 이벤트 타입 문자열을 노출한다")
    void 생성_정상_필드와_이벤트타입() {
        // given
        LocalDateTime confirmedAt = LocalDateTime.of(2026, 7, 24, 10, 0);

        // when
        TradeConfirmedEvent event = new TradeConfirmedEvent(1010L, confirmedAt);

        // then
        assertThat(event.getAuctionId()).isEqualTo(1010L);
        assertThat(event.getConfirmedAt()).isEqualTo(confirmedAt);
        assertThat(event.getEventType()).isEqualTo("trade.confirmed");
    }

    @Test
    @DisplayName("경매 id나 확정시각이 null이면 생성 시점에 예외를 던진다")
    void 생성_null_필수값은_예외() {
        // given-when-then
        assertThatThrownBy(() -> new TradeConfirmedEvent(null, LocalDateTime.now()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new TradeConfirmedEvent(1010L, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
