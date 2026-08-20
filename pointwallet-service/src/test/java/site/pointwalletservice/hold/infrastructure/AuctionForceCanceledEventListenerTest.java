package site.pointwalletservice.hold.infrastructure;
import static org.mockito.Mockito.verify;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import site.common.event.contract.AuctionForceCanceledEvent;
import site.pointwalletservice.hold.application.HoldService;


@ExtendWith(MockitoExtension.class)
@DisplayName("AuctionForceCanceledEventListener")
class AuctionForceCanceledEventListenerTest {

    @Mock
    private HoldService holdService;

    private AuctionForceCanceledEventListener sut;

    private static final Long AUCTION_ID = 5001L;

    @BeforeEach
    void setUp() {
        sut = new AuctionForceCanceledEventListener(holdService);
    }

    @Test
    @DisplayName("이벤트를 받으면 auctionId로 release를 호출한다")
    void handle_release_위임() {
        // given
        AuctionForceCanceledEvent event = AuctionForceCanceledEvent.builder()
                .auctionId(AUCTION_ID)
                .bidderId(456L)
                .build();

        // when
        sut.handle(event);

        // then
        verify(holdService).release(AUCTION_ID);
    }
}