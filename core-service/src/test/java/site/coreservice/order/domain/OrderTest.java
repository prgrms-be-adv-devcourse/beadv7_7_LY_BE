package site.coreservice.order.domain;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import site.coreservice.order.exception.OrderException;

@DisplayName("Order")
class OrderTest {

    private static OrderItemSnapshot defaultItemSnapshot() {
        return OrderItemSnapshot.of(
                "Abbey Road", "비틀즈", 1969, "ORIGINAL",
                ConditionGrade.VERY_GOOD_PLUS, "https://cdn.example.com/listings/5001/photo1.jpg");
    }

    private static DeliveryInfo defaultDeliveryInfo() {
        return DeliveryInfo.of("홍길동", "010-1234-5678", "서울시 강남구", "101동 202호");
    }

    private static Order pendingOrder() {
        return Order.of(5001L, 1201L, 301L, 302L, BigDecimal.valueOf(85_000),
                LocalDateTime.now().plusHours(24), defaultItemSnapshot());
    }

    @Nested
    @DisplayName("생성")
    class Creation {

        @Test
        @DisplayName("생성하면 PENDING 상태로 시작한다")
        void createStartsAsPending() {
            // given & when
            Order order = pendingOrder();

            // then
            assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        }

        @Test
        @DisplayName("생성 시 전달한 값들이 그대로 저장된다")
        void createStoresGivenValues() {
            // given
            LocalDateTime deadline = LocalDateTime.now().plusHours(24);
            OrderItemSnapshot itemSnapshot = defaultItemSnapshot();

            // when
            Order order = Order.of(5001L, 1201L, 301L, 302L, BigDecimal.valueOf(85_000), deadline, itemSnapshot);

            // then
            assertThat(order.getAuctionId()).isEqualTo(5001L);
            assertThat(order.getProductId()).isEqualTo(1201L);
            assertThat(order.getBuyerId()).isEqualTo(301L);
            assertThat(order.getSellerId()).isEqualTo(302L);
            assertThat(order.getFinalBidPrice()).isEqualByComparingTo(BigDecimal.valueOf(85_000));
            assertThat(order.getOrderDeadline()).isEqualTo(deadline);
            assertThat(order.getItemSnapshot()).isEqualTo(itemSnapshot);
        }

        @Test
        @DisplayName("생성 시점에는 확정/취소/완료 관련 값이 비어 있다")
        void createLeavesLifecycleFieldsEmpty() {
            // given & when
            Order order = pendingOrder();

            // then
            assertThat(order.getOrderedAt()).isNull();
            assertThat(order.getCancelledAt()).isNull();
            assertThat(order.getCompletedAt()).isNull();
            assertThat(order.getCancelReason()).isNull();
            assertThat(order.getDeliveryInfo()).isNull();
        }
    }

    @Nested
    @DisplayName("주문 확정 (confirmOrder)")
    class ConfirmOrder {

        @Test
        @DisplayName("PENDING 상태에서 배송지와 함께 확정하면 ORDERED로 바뀐다")
        void confirmFromPending() {
            // given
            Order order = pendingOrder();
            DeliveryInfo deliveryInfo = defaultDeliveryInfo();
            LocalDateTime completionDeadline = LocalDateTime.now().plusDays(7);
            LocalDateTime now = LocalDateTime.now();

            // when
            order.confirmOrder(deliveryInfo, completionDeadline, now);

            // then
            assertThat(order.getStatus()).isEqualTo(OrderStatus.ORDERED);
            assertThat(order.getOrderedAt()).isEqualTo(now);
            assertThat(order.getCompletionDeadline()).isEqualTo(completionDeadline);
            assertThat(order.getDeliveryInfo()).isEqualTo(deliveryInfo);
        }

        @Test
        @DisplayName("배송지가 없으면 예외가 발생한다")
        void confirmWithoutDeliveryInfo_throwsException() {
            // given
            Order order = pendingOrder();

            // when & then
            assertThatThrownBy(() -> order.confirmOrder(null, LocalDateTime.now().plusDays(7), LocalDateTime.now()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("배송지 정보는 필수입니다.");
        }

        @Test
        @DisplayName("PENDING이 아니면 예외가 발생한다")
        void confirmWhenNotPending_throwsException() {
            // given
            Order order = pendingOrder();
            order.confirmOrder(defaultDeliveryInfo(), LocalDateTime.now().plusDays(7), LocalDateTime.now());

            // when & then
            assertThatThrownBy(() -> order.confirmOrder(defaultDeliveryInfo(), LocalDateTime.now().plusDays(7), LocalDateTime.now()))
                    .isInstanceOf(OrderException.class)
                    .hasMessage("PENDING 상태의 주문만 확정할 수 있습니다");
        }

        @Test
        @DisplayName("주문 확정 기한 정각에 확정하면 ORDERED로 바뀐다")
        void confirmExactlyAtDeadline_succeeds() {
            // given
            LocalDateTime deadline = LocalDateTime.now().plusHours(24);
            Order order = Order.of(5001L, 1201L, 301L, 302L, BigDecimal.valueOf(85_000),
                    deadline, defaultItemSnapshot());

            // when
            order.confirmOrder(defaultDeliveryInfo(), LocalDateTime.now().plusDays(7), deadline);

            // then
            assertThat(order.getStatus()).isEqualTo(OrderStatus.ORDERED);
        }

        @Test
        @DisplayName("주문 확정 기한이 지나면 예외가 발생한다")
        void confirmAfterDeadline_throwsException() {
            // given
            LocalDateTime deadline = LocalDateTime.now().plusHours(24);
            Order order = Order.of(5001L, 1201L, 301L, 302L, BigDecimal.valueOf(85_000),
                    deadline, defaultItemSnapshot());

            // when & then
            assertThatThrownBy(() -> order.confirmOrder(
                    defaultDeliveryInfo(), LocalDateTime.now().plusDays(7), deadline.plusSeconds(1)))
                    .isInstanceOf(OrderException.class)
                    .hasMessage("주문 확정 기한이 지났습니다");
        }
    }

    @Nested
    @DisplayName("주문 취소 (cancelOrder)")
    class CancelOrder {

        @Test
        @DisplayName("PENDING 상태에서 취소하면 CANCELLED로 바뀐다")
        void cancelFromPending() {
            // given
            Order order = pendingOrder();

            // when
            order.cancelOrder(CancelReason.BUYER_DECLINED);

            // then
            assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
            assertThat(order.getCancelledAt()).isNotNull();
            assertThat(order.getCancelReason()).isEqualTo(CancelReason.BUYER_DECLINED);
        }

        @Test
        @DisplayName("스케줄러에 의한 타임아웃 취소 사유도 기록할 수 있다")
        void cancelWithConfirmationTimeout() {
            // given
            Order order = pendingOrder();

            // when
            order.cancelOrder(CancelReason.CONFIRMATION_TIMEOUT);

            // then
            assertThat(order.getCancelReason()).isEqualTo(CancelReason.CONFIRMATION_TIMEOUT);
        }

        @Test
        @DisplayName("ORDERED 상태에서는 취소할 수 없다")
        void cancelAfterOrdered_throwsException() {
            // given
            Order order = pendingOrder();
            order.confirmOrder(defaultDeliveryInfo(), LocalDateTime.now().plusDays(7), LocalDateTime.now());

            // when & then
            assertThatThrownBy(() -> order.cancelOrder(CancelReason.BUYER_DECLINED))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("거래 확정 (completeOrder)")
    class CompleteOrder {

        @Test
        @DisplayName("ORDERED 상태에서 완료하면 COMPLETED로 바뀐다")
        void completeFromOrdered() {
            // given
            Order order = pendingOrder();
            order.confirmOrder(defaultDeliveryInfo(), LocalDateTime.now().plusDays(7), LocalDateTime.now());

            // when
            order.completeOrder();

            // then
            assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
            assertThat(order.getCompletedAt()).isNotNull();
        }

        @Test
        @DisplayName("ORDERED가 아니면 예외가 발생한다")
        void completeWhenNotOrdered_throwsException() {
            // given
            Order order = pendingOrder();

            // when & then
            assertThatThrownBy(order::completeOrder)
                    .isInstanceOf(IllegalStateException.class);
        }
    }
}
