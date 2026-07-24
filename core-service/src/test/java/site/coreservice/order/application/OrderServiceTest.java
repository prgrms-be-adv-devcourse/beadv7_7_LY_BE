package site.coreservice.order.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
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
import site.coreservice.order.domain.DeliveryInfo;
import site.coreservice.order.domain.Order;
import site.coreservice.order.domain.OrderItemSnapshot;
import site.coreservice.order.domain.OrderRepository;
import site.coreservice.order.domain.OrderStatus;
import site.coreservice.order.application.port.ProductInfo;
import site.coreservice.order.application.port.ProductPort;
import site.coreservice.order.exception.OrderException;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService")
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductPort productInfoPort;

    @InjectMocks
    private OrderService orderService;

    @Captor
    private ArgumentCaptor<Order> orderCaptor;

    private AuctionWonEvent auctionWonEvent;
    private ProductInfo productInfo;

    @BeforeEach
    void setUp() {
        auctionWonEvent = new AuctionWonEvent(
                5001L, 1201L, 301L, 302L, "VERY_GOOD_PLUS",
                "https://cdn.example.com/listings/5001/photo1.jpg", BigDecimal.valueOf(85_000));

        productInfo = new ProductInfo("Abbey Road", "비틀즈", 1969, "ORIGINAL");
    }

    @Nested
    @DisplayName("createOrder")
    class CreateOrder {

        @Test
        @DisplayName("낙찰 이벤트와 상품 스냅샷을 합성해 PENDING 주문을 생성한다")
        void createsPendingOrderFromEventAndProductSnapshot() {
            // given
            given(productInfoPort.getProductInfo(1201L)).willReturn(productInfo);
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
            verify(productInfoPort, never()).getProductInfo(anyLong());
            verify(orderRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("placeOrder")
    class PlaceOrder {

        private final DeliveryInfo deliveryInfo = DeliveryInfo.of("홍길동", "010-1234-5678", "서울시 강남구", "101동 202호");

        private Order pendingOrder() {
            OrderItemSnapshot itemSnapshot = OrderItemSnapshot.of(
                    "Abbey Road", "비틀즈", 1969, "ORIGINAL",
                    ConditionGrade.VERY_GOOD_PLUS, "https://cdn.example.com/listings/5001/photo1.jpg");
            return Order.of(5001L, 1201L, 301L, 302L, BigDecimal.valueOf(85_000),
                    LocalDateTime.now().plusHours(24), itemSnapshot);
        }

        @Test
        @DisplayName("본인 주문에 배송지를 입력하면 ORDERED로 바뀐다")
        void placesOrderForOwningBuyer() {
            // given
            Order order = pendingOrder();
            given(orderRepository.findById(1L)).willReturn(Optional.of(order));

            // when
            orderService.placeOrder(1L, 301L, deliveryInfo);

            // then
            assertThat(order.getStatus()).isEqualTo(OrderStatus.ORDERED);
            assertThat(order.getDeliveryInfo()).isEqualTo(deliveryInfo);
        }

        @Test
        @DisplayName("존재하지 않는 주문이면 예외가 발생한다")
        void throwsWhenOrderNotFound() {
            // given
            given(orderRepository.findById(1L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> orderService.placeOrder(1L, 301L, deliveryInfo))
                    .isInstanceOf(OrderException.class);
        }

        @Test
        @DisplayName("주문의 구매자가 아니면 예외가 발생한다")
        void throwsWhenNotOrderBuyer() {
            // given
            Order order = pendingOrder();
            given(orderRepository.findById(1L)).willReturn(Optional.of(order));

            // when & then
            assertThatThrownBy(() -> orderService.placeOrder(1L, 999L, deliveryInfo))
                    .isInstanceOf(OrderException.class);
        }

        @Test
        @DisplayName("배송지 주소가 없으면 예외가 발생한다")
        void throwsWhenBaseAddressMissing() {
            // given
            Order order = pendingOrder();
            given(orderRepository.findById(1L)).willReturn(Optional.of(order));
            DeliveryInfo blankAddress = DeliveryInfo.of("홍길동", "010-1234-5678", null, null);

            // when & then
            assertThatThrownBy(() -> orderService.placeOrder(1L, 301L, blankAddress))
                    .isInstanceOf(OrderException.class);
        }
    }
}
