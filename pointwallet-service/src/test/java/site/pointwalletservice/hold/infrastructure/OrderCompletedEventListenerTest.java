package site.pointwalletservice.hold.infrastructure;
import static org.mockito.Mockito.verify;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import site.common.event.contract.OrderCompletedEvent;
import site.pointwalletservice.hold.application.HoldService;


@ExtendWith(MockitoExtension.class)
@DisplayName("OrderCompletedEventListener")
class OrderCompletedEventListenerTest {

    @Mock
    private HoldService holdService;

    private OrderCompletedEventListener sut;

    private static final Long AUCTION_ID = 5001L;

    @BeforeEach
    void setUp() {
        sut = new OrderCompletedEventListener(holdService);
    }

    @Test
    @DisplayName("이벤트를 받으면 auctionId로 consume을 호출한다")
    void handle_consume_위임() {
        // given
        OrderCompletedEvent event = OrderCompletedEvent.builder()
                .orderId(1L)
                .auctionId(AUCTION_ID)
                .buyerId(456L)
                .sellerId(789L)
                .finalBidPrice(BigDecimal.valueOf(15_000))
                .completedAt(LocalDateTime.now())
                .build();

        // when
        sut.handle(event);

        // then
        verify(holdService).consume(AUCTION_ID);
    }
}