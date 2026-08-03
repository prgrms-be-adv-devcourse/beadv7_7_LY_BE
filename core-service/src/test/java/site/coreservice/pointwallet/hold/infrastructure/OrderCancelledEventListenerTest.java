package site.coreservice.pointwallet.hold.infrastructure;
import static org.mockito.Mockito.verify;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import site.common.event.contract.OrderCancelledEvent;
import site.coreservice.pointwallet.hold.application.HoldService;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderCancelledEventListener")
class OrderCancelledEventListenerTest {

    @Mock
    private HoldService holdService;

    private OrderCancelledEventListener sut;

    private static final Long AUCTION_ID = 5001L;

    @BeforeEach
    void setUp() {
        sut = new OrderCancelledEventListener(holdService);
    }

    @Test
    @DisplayName("이벤트를 받으면 auctionId로 release를 호출한다")
    void handle_release_위임() {
        // given
        OrderCancelledEvent event = OrderCancelledEvent.builder()
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