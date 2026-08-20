package site.pointwalletservice.hold.infrastructure;
import static org.mockito.Mockito.verify;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import site.common.event.contract.OrderRefundedEvent;
import site.pointwalletservice.hold.application.HoldService;


@ExtendWith(MockitoExtension.class)
@DisplayName("OrderRefundedEventListener")
class OrderRefundedEventListenerTest {

    @Mock
    private HoldService holdService;

    private OrderRefundedEventListener sut;

    private static final Long AUCTION_ID = 5001L;

    @BeforeEach
    void setUp() {
        sut = new OrderRefundedEventListener(holdService);
    }

    @Test
    @DisplayName("이벤트를 받으면 auctionId로 release를 호출한다")
    void handle_release_위임() {
        // given
        OrderRefundedEvent event = OrderRefundedEvent.builder()
                .orderId(1L)
                .auctionId(AUCTION_ID)
                .buyerId(456L)
                .build();

        // when
        sut.handle(event);

        // then
        verify(holdService).release(AUCTION_ID);
    }
}