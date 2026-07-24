package site.coreservice.order.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import site.coreservice.auction.domain.AuctionWonEvent;
import site.coreservice.order.domain.ConditionGrade;
import site.coreservice.order.domain.Order;
import site.coreservice.order.domain.OrderRepository;
import site.coreservice.product.application.ProductService;
import site.coreservice.product.application.dto.ProductSnapshotResult;
import site.coreservice.product.domain.PressType;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService")
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductService productService;

    @InjectMocks
    private OrderService orderService;

    @Captor
    private ArgumentCaptor<Order> orderCaptor;

    private AuctionWonEvent auctionWonEvent;
    private ProductSnapshotResult productSnapshot;

    @BeforeEach
    void setUp() {
        auctionWonEvent = new AuctionWonEvent(
                5001L, 1201L, 301L, 302L, "VERY_GOOD_PLUS",
                "https://cdn.example.com/listings/5001/photo1.jpg", BigDecimal.valueOf(85_000));

        productSnapshot = new ProductSnapshotResult(
                1201L, "Abbey Road", "비틀즈", "https://cdn.example.com/cover.jpg",
                "Rock", PressType.ORIGINAL, 1969, true, null);
    }

    @Nested
    @DisplayName("createOrder")
    class CreateOrder {

        @Test
        @DisplayName("낙찰 이벤트와 상품 스냅샷을 합성해 PENDING 주문을 생성한다")
        void createsPendingOrderFromEventAndProductSnapshot() {
            // given
            given(productService.getProductSnapshot(1201L)).willReturn(productSnapshot);
            given(orderRepository.save(orderCaptor.capture())).willAnswer(invocation -> invocation.getArgument(0));

            // when
            orderService.createOrder(auctionWonEvent);

            // then
            Order savedOrder = orderCaptor.getValue();
            assertThat(savedOrder.getAuctionId()).isEqualTo(5001L);
            assertThat(savedOrder.getProductId()).isEqualTo(1201L);
            assertThat(savedOrder.getBuyerId()).isEqualTo(301L);
            assertThat(savedOrder.getSellerId()).isEqualTo(302L);
            assertThat(savedOrder.getFinalBidPrice()).isEqualByComparingTo(BigDecimal.valueOf(85_000));
            assertThat(savedOrder.getOrderDeadline()).isAfter(java.time.LocalDateTime.now().plusHours(23));
            assertThat(savedOrder.getItemSnapshot().getAlbumTitle()).isEqualTo("Abbey Road");
            assertThat(savedOrder.getItemSnapshot().getArtistName()).isEqualTo("비틀즈");
            assertThat(savedOrder.getItemSnapshot().getConditionGrade()).isEqualTo(ConditionGrade.VERY_GOOD_PLUS);
            assertThat(savedOrder.getItemSnapshot().getRepresentativeImageUrl())
                    .isEqualTo("https://cdn.example.com/listings/5001/photo1.jpg");
        }

        @Test
        @DisplayName("이미 같은 auctionId로 주문이 생성되어 있으면 중복 생성을 건너뛴다")
        void skipsWhenOrderAlreadyExistsForAuction() {
            // given
            given(orderRepository.existsByAuctionId(5001L)).willReturn(true);

            // when
            orderService.createOrder(auctionWonEvent);

            // then
            verify(productService, never()).getProductSnapshot(anyLong());
            verify(orderRepository, never()).save(any());
        }
    }
}
