package site.fulfillmentservice.order.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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
import site.common.event.contract.AuctionWonEvent;
import site.fulfillmentservice.order.application.dto.OrderDetailResult;
import site.fulfillmentservice.order.application.dto.OrderSearchResult;
import site.fulfillmentservice.order.application.dto.RefundRequestCommand;
import site.fulfillmentservice.order.domain.CancelReason;
import site.fulfillmentservice.order.domain.DeliveryInfo;
import site.fulfillmentservice.order.domain.Order;
import site.fulfillmentservice.order.domain.OrderItemSnapshot;
import site.fulfillmentservice.order.domain.OrderRepository;
import site.fulfillmentservice.order.domain.OrderSearchPage;
import site.fulfillmentservice.order.domain.OrderStatus;
import site.fulfillmentservice.order.domain.RefundReason;
import site.fulfillmentservice.order.application.port.ProductInfo;
import site.fulfillmentservice.order.application.port.ProductPort;
import site.fulfillmentservice.order.exception.OrderException;

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

    private static OrderItemSnapshot defaultItemSnapshot() {
        return OrderItemSnapshot.of(
                "Abbey Road", "비틀즈", 1969, "ORIGINAL",
                "VERY_GOOD_PLUS", "https://cdn.example.com/listings/5001/photo1.jpg");
    }

    private static DeliveryInfo defaultDeliveryInfo() {
        return DeliveryInfo.of("홍길동", "010-1234-5678", "서울시 강남구", "101동 202호");
    }

    @BeforeEach
    void setUp() {
        auctionWonEvent = AuctionWonEvent.builder()
                .auctionId(5001L)
                .productId(1201L)
                .winnerId(301L)
                .sellerId(302L)
                .itemCondition("VERY_GOOD_PLUS")
                .firstImageUrl("https://cdn.example.com/listings/5001/photo1.jpg")
                .winningPrice(BigDecimal.valueOf(85_000))
                .build();

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

        private final DeliveryInfo deliveryInfo = defaultDeliveryInfo();

        private Order pendingOrder() {
            return Order.of(5001L, 1201L, 301L, 302L, BigDecimal.valueOf(85_000),
                    LocalDateTime.now().plusHours(24), defaultItemSnapshot());
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
            return Order.of(5001L, 1201L, 301L, 302L, BigDecimal.valueOf(85_000),
                    LocalDateTime.now().plusHours(24), defaultItemSnapshot());
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
            order.confirmOrder(defaultDeliveryInfo(), LocalDateTime.now().plusDays(7), LocalDateTime.now());
            given(orderRepository.findById(1L)).willReturn(Optional.of(order));

            // when & then
            assertThatThrownBy(() -> orderService.cancelOrder(1L, 301L))
                    .isInstanceOf(OrderException.class);
            verify(orderEventPublisher, never()).publishCancelled(any());
        }

        @Test
        @DisplayName("주문 취소 가능 기한이 지나면 예외가 발생한다")
        void throwsWhenCancelDeadlineExpired() {
            // given
            Order order = Order.of(5001L, 1201L, 301L, 302L, BigDecimal.valueOf(85_000),
                    LocalDateTime.now().minusMinutes(1), defaultItemSnapshot());
            given(orderRepository.findById(1L)).willReturn(Optional.of(order));

            // when & then
            assertThatThrownBy(() -> orderService.cancelOrder(1L, 301L))
                    .isInstanceOf(OrderException.class)
                    .hasMessage("주문 취소 가능 기한이 지났습니다");
            verify(orderEventPublisher, never()).publishCancelled(any());
        }

        @Test
        @DisplayName("이미 취소된 주문이면 기한도 지났더라도 상태 오류가 우선한다")
        void throwsOrderNotCancellableEvenWhenDeadlineAlsoExpired() {
            // given
            Order order = Order.of(5001L, 1201L, 301L, 302L, BigDecimal.valueOf(85_000),
                    LocalDateTime.now().minusMinutes(1), defaultItemSnapshot());
            order.cancelByTimeout(LocalDateTime.now());
            given(orderRepository.findById(1L)).willReturn(Optional.of(order));

            // when & then
            assertThatThrownBy(() -> orderService.cancelOrder(1L, 301L))
                    .isInstanceOf(OrderException.class)
                    .hasMessage("취소할 수 없는 주문 상태입니다");
            verify(orderEventPublisher, never()).publishCancelled(any());
        }
    }

    @Nested
    @DisplayName("findExpiredOrderIds")
    class FindExpiredOrderIds {

        private Order pendingOrder(Long id) {
            Order order = Order.of(5001L, 1201L, 301L, 302L, BigDecimal.valueOf(85_000),
                    LocalDateTime.now().minusHours(1), defaultItemSnapshot());
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

        @Test
        @DisplayName("현재 시각보다 1분 이전 시각을 조회 기준으로 사용한다")
        void usesGracePeriodThreshold() {
            // given
            given(orderRepository.findAllByStatusAndOrderDeadlineBefore(eq(OrderStatus.PENDING), any()))
                    .willReturn(List.of());
            ArgumentCaptor<LocalDateTime> thresholdCaptor = ArgumentCaptor.forClass(LocalDateTime.class);

            // when
            LocalDateTime before = LocalDateTime.now();
            orderService.findExpiredOrderIds();
            LocalDateTime after = LocalDateTime.now();

            // then
            verify(orderRepository).findAllByStatusAndOrderDeadlineBefore(eq(OrderStatus.PENDING), thresholdCaptor.capture());
            assertThat(thresholdCaptor.getValue())
                    .isBetween(before.minusMinutes(1).minusSeconds(2), after.minusMinutes(1).plusSeconds(2));
        }
    }

    @Nested
    @DisplayName("cancelExpiredOrder")
    class CancelExpiredOrder {

        private Order pendingOrder() {
            Order order = Order.of(5001L, 1201L, 301L, 302L, BigDecimal.valueOf(85_000),
                    LocalDateTime.now().minusHours(1), defaultItemSnapshot());
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
            Order order = Order.of(5001L, 1201L, 301L, 302L, BigDecimal.valueOf(85_000),
                    LocalDateTime.now().plusHours(24), defaultItemSnapshot());
            order.cancelByBuyer(LocalDateTime.now());
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
            Order order = Order.of(5001L, 1201L, 301L, 302L, BigDecimal.valueOf(85_000),
                    LocalDateTime.now().plusHours(24), defaultItemSnapshot());
            order.confirmOrder(defaultDeliveryInfo(), LocalDateTime.now().plusDays(7), LocalDateTime.now());
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
            Order order = Order.of(5001L, 1201L, 301L, 302L, BigDecimal.valueOf(85_000),
                    LocalDateTime.now().plusHours(24), defaultItemSnapshot());
            given(orderRepository.findById(1L)).willReturn(Optional.of(order));

            // when & then
            assertThatThrownBy(() -> orderService.completeOrder(1L, 301L))
                    .isInstanceOf(OrderException.class);
            verify(orderEventPublisher, never()).publishCompleted(any());
        }

        @Test
        @DisplayName("거래 확정 가능 기한이 지나면 예외가 발생한다")
        void throwsWhenCompletionDeadlineExpired() {
            // given
            Order order = Order.of(5001L, 1201L, 301L, 302L, BigDecimal.valueOf(85_000),
                    LocalDateTime.now().plusHours(24), defaultItemSnapshot());
            order.confirmOrder(defaultDeliveryInfo(), LocalDateTime.now().minusMinutes(1), LocalDateTime.now());
            given(orderRepository.findById(1L)).willReturn(Optional.of(order));

            // when & then
            assertThatThrownBy(() -> orderService.completeOrder(1L, 301L))
                    .isInstanceOf(OrderException.class)
                    .hasMessage("거래 확정 기한이 지났습니다");
            verify(orderEventPublisher, never()).publishCompleted(any());
        }

        @Test
        @DisplayName("이미 거래 확정된 주문이면 기한도 지났더라도 상태 오류가 우선한다")
        void throwsOrderNotOrderedEvenWhenDeadlineAlsoExpired() {
            // given
            Order order = Order.of(5001L, 1201L, 301L, 302L, BigDecimal.valueOf(85_000),
                    LocalDateTime.now().plusHours(24), defaultItemSnapshot());
            order.confirmOrder(defaultDeliveryInfo(), LocalDateTime.now().minusMinutes(1), LocalDateTime.now());
            order.completeByTimeout(LocalDateTime.now());
            given(orderRepository.findById(1L)).willReturn(Optional.of(order));

            // when & then
            assertThatThrownBy(() -> orderService.completeOrder(1L, 301L))
                    .isInstanceOf(OrderException.class)
                    .hasMessage("ORDERED 상태의 주문만 거래 확정할 수 있습니다");
            verify(orderEventPublisher, never()).publishCompleted(any());
        }
    }

    @Nested
    @DisplayName("findOrdersToAutoComplete")
    class FindOrdersToAutoComplete {

        private Order orderedOrder(Long id) {
            Order order = Order.of(5001L, 1201L, 301L, 302L, BigDecimal.valueOf(85_000),
                    LocalDateTime.now().plusHours(24), defaultItemSnapshot());
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

        @Test
        @DisplayName("현재 시각보다 1분 이전 시각을 조회 기준으로 사용한다")
        void usesGracePeriodThreshold() {
            // given
            given(orderRepository.findAllByStatusAndCompletionDeadlineBefore(eq(OrderStatus.ORDERED), any()))
                    .willReturn(List.of());
            ArgumentCaptor<LocalDateTime> thresholdCaptor = ArgumentCaptor.forClass(LocalDateTime.class);

            // when
            LocalDateTime before = LocalDateTime.now();
            orderService.findOrdersToAutoComplete();
            LocalDateTime after = LocalDateTime.now();

            // then
            verify(orderRepository).findAllByStatusAndCompletionDeadlineBefore(eq(OrderStatus.ORDERED), thresholdCaptor.capture());
            assertThat(thresholdCaptor.getValue())
                    .isBetween(before.minusMinutes(1).minusSeconds(2), after.minusMinutes(1).plusSeconds(2));
        }
    }

    @Nested
    @DisplayName("completeExpiredOrder")
    class CompleteExpiredOrder {

        private Order orderedOrder() {
            Order order = Order.of(5001L, 1201L, 301L, 302L, BigDecimal.valueOf(85_000),
                    LocalDateTime.now().plusHours(24), defaultItemSnapshot());
            order.confirmOrder(defaultDeliveryInfo(), LocalDateTime.now().plusDays(7), LocalDateTime.now());
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
            order.completeByTimeout(LocalDateTime.now());
            given(orderRepository.findById(1L)).willReturn(Optional.of(order));

            // when
            orderService.completeExpiredOrder(1L);

            // then
            verify(orderEventPublisher, never()).publishCompleted(any());
        }
    }

    @Nested
    @DisplayName("requestRefund")
    class RequestRefund {

        private final RefundRequestCommand command = new RefundRequestCommand(
                RefundReason.DEFECTIVE, "박스가 파손되어 도착했습니다", List.of("https://cdn.example.com/refund/1.jpg"));

        private Order orderedOrder() {
            Order order = Order.of(5001L, 1201L, 301L, 302L, BigDecimal.valueOf(85_000),
                    LocalDateTime.now().plusHours(24), defaultItemSnapshot());
            order.confirmOrder(defaultDeliveryInfo(), LocalDateTime.now().plusDays(7), LocalDateTime.now());
            return order;
        }

        @Test
        @DisplayName("본인 주문에 환불을 신청하면 REFUND_REQUESTED로 바뀐다")
        void requestsRefundForOwningBuyer() {
            // given
            Order order = orderedOrder();
            given(orderRepository.findById(1L)).willReturn(Optional.of(order));

            // when
            orderService.requestRefund(1L, 301L, command);

            // then
            assertThat(order.getStatus()).isEqualTo(OrderStatus.REFUND_REQUESTED);
        }

        @Test
        @DisplayName("존재하지 않는 주문이면 예외가 발생한다")
        void throwsWhenOrderNotFound() {
            // given
            given(orderRepository.findById(1L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> orderService.requestRefund(1L, 301L, command))
                    .isInstanceOf(OrderException.class);
        }

        @Test
        @DisplayName("주문의 구매자가 아니면 예외가 발생한다")
        void throwsWhenNotOrderBuyer() {
            // given
            Order order = orderedOrder();
            given(orderRepository.findById(1L)).willReturn(Optional.of(order));

            // when & then
            assertThatThrownBy(() -> orderService.requestRefund(1L, 999L, command))
                    .isInstanceOf(OrderException.class);
        }

        @Test
        @DisplayName("ORDERED 상태가 아니면 예외가 발생한다")
        void throwsWhenOrderNotRefundable() {
            // given
            Order order = Order.of(5001L, 1201L, 301L, 302L, BigDecimal.valueOf(85_000),
                    LocalDateTime.now().plusHours(24), defaultItemSnapshot());
            given(orderRepository.findById(1L)).willReturn(Optional.of(order));

            // when & then
            assertThatThrownBy(() -> orderService.requestRefund(1L, 301L, command))
                    .isInstanceOf(OrderException.class);
        }

        @Test
        @DisplayName("환불 신청 가능 기한이 지나면 예외가 발생한다")
        void throwsWhenRefundRequestDeadlineExpired() {
            // given
            Order order = Order.of(5001L, 1201L, 301L, 302L, BigDecimal.valueOf(85_000),
                    LocalDateTime.now().plusHours(24), defaultItemSnapshot());
            order.confirmOrder(defaultDeliveryInfo(), LocalDateTime.now().minusMinutes(1), LocalDateTime.now());
            given(orderRepository.findById(1L)).willReturn(Optional.of(order));

            // when & then
            assertThatThrownBy(() -> orderService.requestRefund(1L, 301L, command))
                    .isInstanceOf(OrderException.class)
                    .hasMessage("환불 신청 가능 기한이 지났습니다");
        }

        @Test
        @DisplayName("이미 거래 확정된 주문이면 기한도 지났더라도 상태 오류가 우선한다")
        void throwsOrderNotRefundableEvenWhenDeadlineAlsoExpired() {
            // given
            Order order = Order.of(5001L, 1201L, 301L, 302L, BigDecimal.valueOf(85_000),
                    LocalDateTime.now().plusHours(24), defaultItemSnapshot());
            order.confirmOrder(defaultDeliveryInfo(), LocalDateTime.now().minusMinutes(1), LocalDateTime.now());
            order.completeByTimeout(LocalDateTime.now());
            given(orderRepository.findById(1L)).willReturn(Optional.of(order));

            // when & then
            assertThatThrownBy(() -> orderService.requestRefund(1L, 301L, command))
                    .isInstanceOf(OrderException.class)
                    .hasMessage("ORDERED 상태의 주문만 환불 신청할 수 있습니다");
        }
    }

    @Nested
    @DisplayName("approveRefund")
    class ApproveRefund {

        private Order refundRequestedOrder() {
            Order order = Order.of(5001L, 1201L, 301L, 302L, BigDecimal.valueOf(85_000),
                    LocalDateTime.now().plusHours(24), defaultItemSnapshot());
            order.confirmOrder(defaultDeliveryInfo(), LocalDateTime.now().plusDays(7), LocalDateTime.now());
            order.requestRefund(RefundReason.DEFECTIVE, "박스가 파손되어 도착했습니다",
                    List.of("https://cdn.example.com/refund/1.jpg"), LocalDateTime.now());
            return order;
        }

        @Test
        @DisplayName("환불 신청을 승인하면 REFUND로 바뀌고 이벤트를 발행한다")
        void approvesRefundRequest() {
            // given
            Order order = refundRequestedOrder();
            given(orderRepository.findById(1L)).willReturn(Optional.of(order));

            // when
            orderService.approveRefund(1L);

            // then
            assertThat(order.getStatus()).isEqualTo(OrderStatus.REFUND);
            verify(orderEventPublisher).publishRefunded(order);
        }

        @Test
        @DisplayName("존재하지 않는 주문이면 예외가 발생한다")
        void throwsWhenOrderNotFound() {
            // given
            given(orderRepository.findById(1L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> orderService.approveRefund(1L))
                    .isInstanceOf(OrderException.class);
            verify(orderEventPublisher, never()).publishRefunded(any());
        }

        @Test
        @DisplayName("REFUND_REQUESTED 상태가 아니면 예외가 발생한다")
        void throwsWhenRefundNotRequested() {
            // given
            Order order = Order.of(5001L, 1201L, 301L, 302L, BigDecimal.valueOf(85_000),
                    LocalDateTime.now().plusHours(24), defaultItemSnapshot());
            given(orderRepository.findById(1L)).willReturn(Optional.of(order));

            // when & then
            assertThatThrownBy(() -> orderService.approveRefund(1L))
                    .isInstanceOf(OrderException.class);
            verify(orderEventPublisher, never()).publishRefunded(any());
        }
    }

    @Nested
    @DisplayName("rejectRefund")
    class RejectRefund {

        private Order refundRequestedOrder() {
            Order order = Order.of(5001L, 1201L, 301L, 302L, BigDecimal.valueOf(85_000),
                    LocalDateTime.now().plusHours(24), defaultItemSnapshot());
            order.confirmOrder(defaultDeliveryInfo(), LocalDateTime.now().plusDays(7), LocalDateTime.now());
            order.requestRefund(RefundReason.DEFECTIVE, "박스가 파손되어 도착했습니다",
                    List.of("https://cdn.example.com/refund/1.jpg"), LocalDateTime.now());
            return order;
        }

        @Test
        @DisplayName("환불 신청을 반려하면 REFUND_REJECTED로 바뀌고 거래 확정 이벤트를 발행한다")
        void rejectsRefundRequest() {
            // given
            Order order = refundRequestedOrder();
            given(orderRepository.findById(1L)).willReturn(Optional.of(order));

            // when
            orderService.rejectRefund(1L);

            // then
            assertThat(order.getStatus()).isEqualTo(OrderStatus.REFUND_REJECTED);
            verify(orderEventPublisher).publishCompleted(order);
        }

        @Test
        @DisplayName("존재하지 않는 주문이면 예외가 발생한다")
        void throwsWhenOrderNotFound() {
            // given
            given(orderRepository.findById(1L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> orderService.rejectRefund(1L))
                    .isInstanceOf(OrderException.class);
            verify(orderEventPublisher, never()).publishCompleted(any());
        }

        @Test
        @DisplayName("REFUND_REQUESTED 상태가 아니면 예외가 발생한다")
        void throwsWhenRefundNotRequested() {
            // given
            Order order = Order.of(5001L, 1201L, 301L, 302L, BigDecimal.valueOf(85_000),
                    LocalDateTime.now().plusHours(24), defaultItemSnapshot());
            given(orderRepository.findById(1L)).willReturn(Optional.of(order));

            // when & then
            assertThatThrownBy(() -> orderService.rejectRefund(1L))
                    .isInstanceOf(OrderException.class);
            verify(orderEventPublisher, never()).publishCompleted(any());
        }
    }

    @Nested
    @DisplayName("getOrderDetail")
    class GetOrderDetail {

        private Order pendingOrder() {
            return Order.of(5001L, 1201L, 301L, 302L, BigDecimal.valueOf(85_000),
                    LocalDateTime.now().plusHours(24), defaultItemSnapshot());
        }

        private Order orderedOrder() {
            Order order = pendingOrder();
            order.confirmOrder(defaultDeliveryInfo(), LocalDateTime.now().plusDays(7), LocalDateTime.now());
            return order;
        }

        @Test
        @DisplayName("환불 이력이 없으면 refundInfo는 null이다")
        void returnsNullRefundInfoWhenNoRefundHistory() {
            // given
            Order order = pendingOrder();
            given(orderRepository.findById(1L)).willReturn(Optional.of(order));

            // when
            OrderDetailResult result = orderService.getOrderDetail(1L, 301L);

            // then
            assertThat(result.refundInfo()).isNull();
        }

        @Test
        @DisplayName("환불 신청 중이면 refundInfo를 포함해 반환한다")
        void returnsRefundInfoWhenRequested() {
            // given
            Order order = orderedOrder();
            order.requestRefund(RefundReason.DEFECTIVE, "박스가 파손되어 도착했습니다",
                    List.of("https://cdn.example.com/refund/1.jpg"), LocalDateTime.now());
            given(orderRepository.findById(1L)).willReturn(Optional.of(order));

            // when
            OrderDetailResult result = orderService.getOrderDetail(1L, 301L);

            // then
            assertThat(result.status()).isEqualTo("REFUND_REQUESTED");
            assertThat(result.refundInfo().reason()).isEqualTo("DEFECTIVE");
            assertThat(result.refundInfo().description()).isEqualTo("박스가 파손되어 도착했습니다");
            assertThat(result.refundInfo().refundedAt()).isNull();
        }

        @Test
        @DisplayName("환불이 승인되면 refundedAt이 채워진 refundInfo를 반환한다")
        void returnsRefundInfoWhenApproved() {
            // given
            Order order = orderedOrder();
            order.requestRefund(RefundReason.DEFECTIVE, null, null, LocalDateTime.now());
            order.approveRefund(LocalDateTime.now());
            given(orderRepository.findById(1L)).willReturn(Optional.of(order));

            // when
            OrderDetailResult result = orderService.getOrderDetail(1L, 301L);

            // then
            assertThat(result.status()).isEqualTo("REFUND");
            assertThat(result.refundInfo().refundedAt()).isNotNull();
        }

        @Test
        @DisplayName("환불이 반려되면 상태는 REFUND_REJECTED이고 refundedAt은 비어있다")
        void returnsRefundInfoWhenRejected() {
            // given
            Order order = orderedOrder();
            order.requestRefund(RefundReason.DEFECTIVE, null, null, LocalDateTime.now());
            order.rejectRefund(LocalDateTime.now());
            given(orderRepository.findById(1L)).willReturn(Optional.of(order));

            // when
            OrderDetailResult result = orderService.getOrderDetail(1L, 301L);

            // then
            assertThat(result.status()).isEqualTo("REFUND_REJECTED");
            assertThat(result.refundInfo().refundedAt()).isNull();
        }

        @Test
        @DisplayName("구매자가 조회하면 주문 상세를 반환한다")
        void returnsDetailForBuyer() {
            // given
            Order order = pendingOrder();
            given(orderRepository.findById(1L)).willReturn(Optional.of(order));

            // when
            OrderDetailResult result = orderService.getOrderDetail(1L, 301L);

            // then
            assertThat(result.buyerId()).isEqualTo(301L);
            assertThat(result.sellerId()).isEqualTo(302L);
        }

        @Test
        @DisplayName("판매자가 조회해도 주문 상세를 반환한다")
        void returnsDetailForSeller() {
            // given
            Order order = pendingOrder();
            given(orderRepository.findById(1L)).willReturn(Optional.of(order));

            // when
            OrderDetailResult result = orderService.getOrderDetail(1L, 302L);

            // then
            assertThat(result.sellerId()).isEqualTo(302L);
        }

        @Test
        @DisplayName("존재하지 않는 주문이면 예외가 발생한다")
        void throwsWhenOrderNotFound() {
            // given
            given(orderRepository.findById(1L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> orderService.getOrderDetail(1L, 301L))
                    .isInstanceOf(OrderException.class);
        }

        @Test
        @DisplayName("구매자도 판매자도 아니면 예외가 발생한다")
        void throwsWhenNeitherBuyerNorSeller() {
            // given
            Order order = pendingOrder();
            given(orderRepository.findById(1L)).willReturn(Optional.of(order));

            // when & then
            assertThatThrownBy(() -> orderService.getOrderDetail(1L, 999L))
                    .isInstanceOf(OrderException.class);
        }
    }

    @Nested
    @DisplayName("getOrderDetailForAdmin")
    class GetOrderDetailForAdmin {

        private Order pendingOrder() {
            return Order.of(5001L, 1201L, 301L, 302L, BigDecimal.valueOf(85_000),
                    LocalDateTime.now().plusHours(24), defaultItemSnapshot());
        }

        @Test
        @DisplayName("주문 상세를 반환한다")
        void returnsDetail() {
            // given
            Order order = pendingOrder();
            given(orderRepository.findById(1L)).willReturn(Optional.of(order));

            // when
            OrderDetailResult result = orderService.getOrderDetailForAdmin(1L);

            // then
            assertThat(result.buyerId()).isEqualTo(301L);
            assertThat(result.sellerId()).isEqualTo(302L);
        }

        @Test
        @DisplayName("존재하지 않는 주문이면 예외가 발생한다")
        void throwsWhenOrderNotFound() {
            // given
            given(orderRepository.findById(1L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> orderService.getOrderDetailForAdmin(1L))
                    .isInstanceOf(OrderException.class);
        }
    }

    @Nested
    @DisplayName("findOrders")
    class FindOrders {

        private Order pendingOrder(Long id) {
            Order order = Order.of(5001L, 1201L, 301L, 302L, BigDecimal.valueOf(85_000),
                    LocalDateTime.now().plusHours(24), defaultItemSnapshot());
            ReflectionTestUtils.setField(order, "id", id);
            return order;
        }

        @Test
        @DisplayName("perspective=buyer면 구매자 기준으로 조회한다")
        void findsByBuyerPerspective() {
            // given
            given(orderRepository.findAllByBuyerId(eq(301L), isNull(), eq(0), eq(20)))
                    .willReturn(new OrderSearchPage(List.of(pendingOrder(1L)), 1L));

            // when
            OrderSearchResult result = orderService.findOrders(301L, "buyer", null, 0, 20);

            // then
            assertThat(result.content()).hasSize(1);
            assertThat(result.totalElements()).isEqualTo(1L);
            verify(orderRepository, never()).findAllBySellerId(any(), any(), anyInt(), anyInt());
        }

        @Test
        @DisplayName("perspective=seller면 판매자 기준으로 조회한다")
        void findsBySellerPerspective() {
            // given
            given(orderRepository.findAllBySellerId(eq(302L), isNull(), eq(0), eq(20)))
                    .willReturn(new OrderSearchPage(List.of(pendingOrder(1L)), 1L));

            // when
            OrderSearchResult result = orderService.findOrders(302L, "seller", null, 0, 20);

            // then
            assertThat(result.content()).hasSize(1);
            verify(orderRepository, never()).findAllByBuyerId(any(), any(), anyInt(), anyInt());
        }

        @Test
        @DisplayName("perspective이 buyer/seller가 아니면 예외가 발생한다")
        void throwsWhenPerspectiveInvalid() {
            // when & then
            assertThatThrownBy(() -> orderService.findOrders(301L, "admin", null, 0, 20))
                    .isInstanceOf(OrderException.class);
        }

        @Test
        @DisplayName("status 문자열이 유효하지 않으면 예외가 발생한다")
        void throwsWhenStatusInvalid() {
            // when & then
            assertThatThrownBy(() -> orderService.findOrders(301L, "buyer", "NOT_A_STATUS", 0, 20))
                    .isInstanceOf(OrderException.class);
        }

        @Test
        @DisplayName("size가 1보다 작으면 기본값(20)으로 대체된다")
        void clampsSizeToDefaultWhenTooSmall() {
            // given
            given(orderRepository.findAllByBuyerId(eq(301L), isNull(), eq(0), eq(20)))
                    .willReturn(new OrderSearchPage(List.of(), 0L));

            // when
            OrderSearchResult result = orderService.findOrders(301L, "buyer", null, 0, 0);

            // then
            assertThat(result.size()).isEqualTo(20);
        }

        @Test
        @DisplayName("page가 음수면 0으로 보정된다")
        void clampsNegativePageToZero() {
            // given
            given(orderRepository.findAllByBuyerId(eq(301L), isNull(), eq(0), eq(20)))
                    .willReturn(new OrderSearchPage(List.of(), 0L));

            // when
            OrderSearchResult result = orderService.findOrders(301L, "buyer", null, -3, 20);

            // then
            assertThat(result.page()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("findOrdersForAdmin")
    class FindOrdersForAdmin {

        private Order pendingOrder(Long id) {
            Order order = Order.of(5001L, 1201L, 301L, 302L, BigDecimal.valueOf(85_000),
                    LocalDateTime.now().plusHours(24), defaultItemSnapshot());
            ReflectionTestUtils.setField(order, "id", id);
            return order;
        }

        @Test
        @DisplayName("status로 필터링해서 조회한다")
        void findsByStatus() {
            // given
            given(orderRepository.findAllByStatus(OrderStatus.REFUND_REQUESTED, 0, 20))
                    .willReturn(new OrderSearchPage(List.of(pendingOrder(1L)), 1L));

            // when
            OrderSearchResult result = orderService.findOrdersForAdmin("REFUND_REQUESTED", 0, 20);

            // then
            assertThat(result.content()).hasSize(1);
            assertThat(result.totalElements()).isEqualTo(1L);
        }

        @Test
        @DisplayName("status가 없으면 전체를 조회한다")
        void findsAllWhenStatusIsNull() {
            // given
            given(orderRepository.findAllByStatus(isNull(), eq(0), eq(20)))
                    .willReturn(new OrderSearchPage(List.of(pendingOrder(1L)), 1L));

            // when
            OrderSearchResult result = orderService.findOrdersForAdmin(null, 0, 20);

            // then
            assertThat(result.content()).hasSize(1);
        }

        @Test
        @DisplayName("status 문자열이 유효하지 않으면 예외가 발생한다")
        void throwsWhenStatusInvalid() {
            // when & then
            assertThatThrownBy(() -> orderService.findOrdersForAdmin("NOT_A_STATUS", 0, 20))
                    .isInstanceOf(OrderException.class);
        }
    }
}
