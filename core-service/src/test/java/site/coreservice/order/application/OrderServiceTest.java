package site.coreservice.order.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
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
import org.springframework.test.util.ReflectionTestUtils;
import site.coreservice.global.event.AuctionWonEvent;
import site.coreservice.order.domain.CancelReason;
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

    @Mock
    private OrderEventPublisher orderEventPublisher;

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
            assertThat(savedOrder.getItemSnapshot().getConditionGrade()).isEqualTo("VERY_GOOD_PLUS");
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
                    "VERY_GOOD_PLUS", "https://cdn.example.com/listings/5001/photo1.jpg");
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
        @DisplayName("배송지 정보가 없으면 예외가 발생한다")
        void throwsWhenDeliveryInfoIsNull() {
            // given
            Order order = pendingOrder();
            given(orderRepository.findById(1L)).willReturn(Optional.of(order));

            // when & then
            assertThatThrownBy(() -> orderService.placeOrder(1L, 301L, null))
                    .isInstanceOf(OrderException.class);
        }
    }

    @Nested
    @DisplayName("cancelOrder")
    class CancelOrder {

        private Order pendingOrder() {
            OrderItemSnapshot itemSnapshot = OrderItemSnapshot.of(
                    "Abbey Road", "비틀즈", 1969, "ORIGINAL",
                    "VERY_GOOD_PLUS", "https://cdn.example.com/listings/5001/photo1.jpg");
            return Order.of(5001L, 1201L, 301L, 302L, BigDecimal.valueOf(85_000),
                    LocalDateTime.now().plusHours(24), itemSnapshot);
        }

        @Test
        @DisplayName("본인 주문을 취소하면 CANCELLED로 바뀐다")
        void cancelsOrderForOwningBuyer() {
            // given
            Order order = pendingOrder();
            given(orderRepository.findById(1L)).willReturn(Optional.of(order));

            // when
            orderService.cancelOrder(1L, 301L);

            // then
            assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
            assertThat(order.getCancelReason()).isEqualTo(CancelReason.BUYER_DECLINED);
            verify(orderEventPublisher).publishCancelled(order);
        }

        @Test
        @DisplayName("존재하지 않는 주문이면 예외가 발생한다")
        void throwsWhenOrderNotFound() {
            // given
            given(orderRepository.findById(1L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> orderService.cancelOrder(1L, 301L))
                    .isInstanceOf(OrderException.class);
            verify(orderEventPublisher, never()).publishCancelled(any());
        }

        @Test
        @DisplayName("주문의 구매자가 아니면 예외가 발생한다")
        void throwsWhenNotOrderBuyer() {
            // given
            Order order = pendingOrder();
            given(orderRepository.findById(1L)).willReturn(Optional.of(order));

            // when & then
            assertThatThrownBy(() -> orderService.cancelOrder(1L, 999L))
                    .isInstanceOf(OrderException.class);
            verify(orderEventPublisher, never()).publishCancelled(any());
        }

        @Test
        @DisplayName("PENDING 상태가 아니면 예외가 발생한다")
        void throwsWhenOrderNotCancellable() {
            // given
            Order order = pendingOrder();
            order.confirmOrder(DeliveryInfo.of("홍길동", "010-1234-5678", "서울시 강남구", "101동 202호"),
                    LocalDateTime.now().plusDays(7), LocalDateTime.now());
            given(orderRepository.findById(1L)).willReturn(Optional.of(order));

            // when & then
            assertThatThrownBy(() -> orderService.cancelOrder(1L, 301L))
                    .isInstanceOf(OrderException.class);
            verify(orderEventPublisher, never()).publishCancelled(any());
        }
    }

    @Nested
    @DisplayName("findExpiredOrderIds")
    class FindExpiredOrderIds {

        private Order pendingOrder(Long id) {
            OrderItemSnapshot itemSnapshot = OrderItemSnapshot.of(
                    "Abbey Road", "비틀즈", 1969, "ORIGINAL",
                    "VERY_GOOD_PLUS", "https://cdn.example.com/listings/5001/photo1.jpg");
            Order order = Order.of(5001L, 1201L, 301L, 302L, BigDecimal.valueOf(85_000),
                    LocalDateTime.now().minusHours(1), itemSnapshot);
            ReflectionTestUtils.setField(order, "id", id);
            return order;
        }

        @Test
        @DisplayName("기한이 지난 PENDING 주문들의 id 목록을 반환한다")
        void returnsExpiredOrderIds() {
            // given
            given(orderRepository.findAllByStatusAndOrderDeadlineBefore(eq(OrderStatus.PENDING), any()))
                    .willReturn(List.of(pendingOrder(1L), pendingOrder(2L)));

            // when
            List<Long> result = orderService.findExpiredOrderIds();

            // then
            assertThat(result).containsExactly(1L, 2L);
        }

        @Test
        @DisplayName("대상이 없으면 빈 목록을 반환한다")
        void returnsEmptyWhenNoExpiredOrders() {
            // given
            given(orderRepository.findAllByStatusAndOrderDeadlineBefore(eq(OrderStatus.PENDING), any()))
                    .willReturn(List.of());

            // when
            List<Long> result = orderService.findExpiredOrderIds();

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("cancelExpiredOrder")
    class CancelExpiredOrder {

        private Order pendingOrder() {
            OrderItemSnapshot itemSnapshot = OrderItemSnapshot.of(
                    "Abbey Road", "비틀즈", 1969, "ORIGINAL",
                    "VERY_GOOD_PLUS", "https://cdn.example.com/listings/5001/photo1.jpg");
            Order order = Order.of(5001L, 1201L, 301L, 302L, BigDecimal.valueOf(85_000),
                    LocalDateTime.now().minusHours(1), itemSnapshot);
            ReflectionTestUtils.setField(order, "id", 1L);
            return order;
        }

        @Test
        @DisplayName("PENDING 주문을 CONFIRMATION_TIMEOUT 사유로 취소하고 이벤트를 발행한다")
        void cancelsPendingOrder() {
            // given
            Order order = pendingOrder();
            given(orderRepository.findById(1L)).willReturn(Optional.of(order));

            // when
            orderService.cancelExpiredOrder(1L);

            // then
            assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
            assertThat(order.getCancelReason()).isEqualTo(CancelReason.CONFIRMATION_TIMEOUT);
            verify(orderEventPublisher).publishCancelled(order);
        }

        @Test
        @DisplayName("이미 존재하지 않는 주문이면 아무 것도 하지 않는다")
        void doesNothingWhenOrderNotFound() {
            // given
            given(orderRepository.findById(1L)).willReturn(Optional.empty());

            // when
            orderService.cancelExpiredOrder(1L);

            // then
            verify(orderEventPublisher, never()).publishCancelled(any());
        }

        @Test
        @DisplayName("이미 PENDING이 아닌 주문이면(레이스로 이미 처리됨) 아무 것도 하지 않는다")
        void doesNothingWhenOrderNoLongerPending() {
            // given
            Order order = pendingOrder();
            order.cancelOrder(CancelReason.BUYER_DECLINED, LocalDateTime.now());
            given(orderRepository.findById(1L)).willReturn(Optional.of(order));

            // when
            orderService.cancelExpiredOrder(1L);

            // then
            assertThat(order.getCancelReason()).isEqualTo(CancelReason.BUYER_DECLINED);
            verify(orderEventPublisher, never()).publishCancelled(any());
        }
    }

    @Nested
    @DisplayName("completeOrder")
    class CompleteOrder {

        private Order orderedOrder() {
            OrderItemSnapshot itemSnapshot = OrderItemSnapshot.of(
                    "Abbey Road", "비틀즈", 1969, "ORIGINAL",
                    "VERY_GOOD_PLUS", "https://cdn.example.com/listings/5001/photo1.jpg");
            Order order = Order.of(5001L, 1201L, 301L, 302L, BigDecimal.valueOf(85_000),
                    LocalDateTime.now().plusHours(24), itemSnapshot);
            order.confirmOrder(DeliveryInfo.of("홍길동", "010-1234-5678", "서울시 강남구", "101동 202호"),
                    LocalDateTime.now().plusDays(7), LocalDateTime.now());
            return order;
        }

        @Test
        @DisplayName("본인 주문을 거래 확정하면 COMPLETED로 바뀐다")
        void completesOrderForOwningBuyer() {
            // given
            Order order = orderedOrder();
            given(orderRepository.findById(1L)).willReturn(Optional.of(order));

            // when
            orderService.completeOrder(1L, 301L);

            // then
            assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
            verify(orderEventPublisher).publishCompleted(order);
        }

        @Test
        @DisplayName("존재하지 않는 주문이면 예외가 발생한다")
        void throwsWhenOrderNotFound() {
            // given
            given(orderRepository.findById(1L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> orderService.completeOrder(1L, 301L))
                    .isInstanceOf(OrderException.class);
            verify(orderEventPublisher, never()).publishCompleted(any());
        }

        @Test
        @DisplayName("주문의 구매자가 아니면 예외가 발생한다")
        void throwsWhenNotOrderBuyer() {
            // given
            Order order = orderedOrder();
            given(orderRepository.findById(1L)).willReturn(Optional.of(order));

            // when & then
            assertThatThrownBy(() -> orderService.completeOrder(1L, 999L))
                    .isInstanceOf(OrderException.class);
            verify(orderEventPublisher, never()).publishCompleted(any());
        }

        @Test
        @DisplayName("ORDERED 상태가 아니면 예외가 발생한다")
        void throwsWhenOrderNotOrdered() {
            // given
            OrderItemSnapshot itemSnapshot = OrderItemSnapshot.of(
                    "Abbey Road", "비틀즈", 1969, "ORIGINAL",
                    "VERY_GOOD_PLUS", "https://cdn.example.com/listings/5001/photo1.jpg");
            Order order = Order.of(5001L, 1201L, 301L, 302L, BigDecimal.valueOf(85_000),
                    LocalDateTime.now().plusHours(24), itemSnapshot);
            given(orderRepository.findById(1L)).willReturn(Optional.of(order));

            // when & then
            assertThatThrownBy(() -> orderService.completeOrder(1L, 301L))
                    .isInstanceOf(OrderException.class);
            verify(orderEventPublisher, never()).publishCompleted(any());
        }
    }

    @Nested
    @DisplayName("findOrdersToAutoComplete")
    class FindOrdersToAutoComplete {

        private Order orderedOrder(Long id) {
            OrderItemSnapshot itemSnapshot = OrderItemSnapshot.of(
                    "Abbey Road", "비틀즈", 1969, "ORIGINAL",
                    "VERY_GOOD_PLUS", "https://cdn.example.com/listings/5001/photo1.jpg");
            Order order = Order.of(5001L, 1201L, 301L, 302L, BigDecimal.valueOf(85_000),
                    LocalDateTime.now().plusHours(24), itemSnapshot);
            ReflectionTestUtils.setField(order, "id", id);
            return order;
        }

        @Test
        @DisplayName("거래 확정 기한이 지난 ORDERED 주문들의 id 목록을 반환한다")
        void returnsOrderIdsToAutoComplete() {
            // given
            given(orderRepository.findAllByStatusAndCompletionDeadlineBefore(eq(OrderStatus.ORDERED), any()))
                    .willReturn(List.of(orderedOrder(1L), orderedOrder(2L)));

            // when
            List<Long> result = orderService.findOrdersToAutoComplete();

            // then
            assertThat(result).containsExactly(1L, 2L);
        }

        @Test
        @DisplayName("대상이 없으면 빈 목록을 반환한다")
        void returnsEmptyWhenNoTargets() {
            // given
            given(orderRepository.findAllByStatusAndCompletionDeadlineBefore(eq(OrderStatus.ORDERED), any()))
                    .willReturn(List.of());

            // when
            List<Long> result = orderService.findOrdersToAutoComplete();

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("completeExpiredOrder")
    class CompleteExpiredOrder {

        private Order orderedOrder() {
            OrderItemSnapshot itemSnapshot = OrderItemSnapshot.of(
                    "Abbey Road", "비틀즈", 1969, "ORIGINAL",
                    "VERY_GOOD_PLUS", "https://cdn.example.com/listings/5001/photo1.jpg");
            Order order = Order.of(5001L, 1201L, 301L, 302L, BigDecimal.valueOf(85_000),
                    LocalDateTime.now().plusHours(24), itemSnapshot);
            order.confirmOrder(DeliveryInfo.of("홍길동", "010-1234-5678", "서울시 강남구", "101동 202호"),
                    LocalDateTime.now().plusDays(7), LocalDateTime.now());
            ReflectionTestUtils.setField(order, "id", 1L);
            return order;
        }

        @Test
        @DisplayName("ORDERED 주문을 COMPLETED로 자동 완료하고 이벤트를 발행한다")
        void completesOrderedOrder() {
            // given
            Order order = orderedOrder();
            given(orderRepository.findById(1L)).willReturn(Optional.of(order));

            // when
            orderService.completeExpiredOrder(1L);

            // then
            assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
            verify(orderEventPublisher).publishCompleted(order);
        }

        @Test
        @DisplayName("존재하지 않는 주문이면 아무 것도 하지 않는다")
        void doesNothingWhenOrderNotFound() {
            // given
            given(orderRepository.findById(1L)).willReturn(Optional.empty());

            // when
            orderService.completeExpiredOrder(1L);

            // then
            verify(orderEventPublisher, never()).publishCompleted(any());
        }

        @Test
        @DisplayName("이미 ORDERED가 아닌 주문이면(레이스로 이미 처리됨) 아무 것도 하지 않는다")
        void doesNothingWhenOrderNoLongerOrdered() {
            // given
            Order order = orderedOrder();
            order.completeOrder(LocalDateTime.now());
            given(orderRepository.findById(1L)).willReturn(Optional.of(order));

            // when
            orderService.completeExpiredOrder(1L);

            // then
            verify(orderEventPublisher, never()).publishCompleted(any());
        }
    }
}
