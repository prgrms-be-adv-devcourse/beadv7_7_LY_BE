package site.fulfillmentservice.order.domain;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import site.fulfillmentservice.order.exception.OrderException;

@DisplayName("Order")
class OrderTest {

    private static OrderItemSnapshot defaultItemSnapshot() {
        return OrderItemSnapshot.of(
                "Abbey Road", "비틀즈", 1969, "ORIGINAL",
                "VERY_GOOD_PLUS", "https://cdn.example.com/listings/5001/photo1.jpg");
    }

    private static DeliveryInfo defaultDeliveryInfo() {
        return DeliveryInfo.of("홍길동", "010-1234-5678", "서울시 강남구", "101동 202호");
    }

    private static Order pendingOrder() {
        return Order.of(5001L, 1201L, 301L, 302L, BigDecimal.valueOf(85_000),
                LocalDateTime.now().plusHours(24), defaultItemSnapshot());
    }

    private static Order orderedOrder() {
        Order order = pendingOrder();
        order.confirmOrder(defaultDeliveryInfo(), LocalDateTime.now().plusDays(7), LocalDateTime.now());
        return order;
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
                    .hasMessage("주문 가능 기한이 지났습니다");
        }
    }

    @Nested
    @DisplayName("주문 취소 (cancelByBuyer)")
    class CancelByBuyer {

        @Test
        @DisplayName("PENDING 상태에서 취소하면 CANCELLED로 바뀐다")
        void cancelFromPending() {
            // given
            Order order = pendingOrder();

            // when
            LocalDateTime now = LocalDateTime.now();
            order.cancelByBuyer(now);

            // then
            assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
            assertThat(order.getCancelledAt()).isEqualTo(now);
            assertThat(order.getCancelReason()).isEqualTo(CancelReason.BUYER_DECLINED);
        }

        @Test
        @DisplayName("ORDERED 상태에서는 취소할 수 없다")
        void cancelAfterOrdered_throwsException() {
            // given
            Order order = pendingOrder();
            order.confirmOrder(defaultDeliveryInfo(), LocalDateTime.now().plusDays(7), LocalDateTime.now());

            // when & then
            assertThatThrownBy(() -> order.cancelByBuyer(LocalDateTime.now()))
                    .isInstanceOf(OrderException.class)
                    .hasMessage("취소할 수 없는 주문 상태입니다");
        }

        @Test
        @DisplayName("주문 취소 가능 기한이 지나면 예외가 발생한다")
        void cancelAfterDeadline_throwsException() {
            // given
            LocalDateTime deadline = LocalDateTime.now().plusHours(24);
            Order order = Order.of(5001L, 1201L, 301L, 302L, BigDecimal.valueOf(85_000),
                    deadline, defaultItemSnapshot());

            // when & then
            assertThatThrownBy(() -> order.cancelByBuyer(deadline.plusSeconds(1)))
                    .isInstanceOf(OrderException.class)
                    .hasMessage("주문 취소 가능 기한이 지났습니다");
        }
    }

    @Nested
    @DisplayName("주문 자동 취소 (cancelByTimeout)")
    class CancelByTimeout {

        @Test
        @DisplayName("PENDING 상태에서 취소하면 CONFIRMATION_TIMEOUT 사유로 CANCELLED로 바뀐다")
        void cancelWithConfirmationTimeout() {
            // given
            Order order = pendingOrder();

            // when
            order.cancelByTimeout(LocalDateTime.now());

            // then
            assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
            assertThat(order.getCancelReason()).isEqualTo(CancelReason.CONFIRMATION_TIMEOUT);
        }

        @Test
        @DisplayName("데드라인이 지났어도 취소할 수 있다")
        void cancelAfterDeadline_succeeds() {
            // given
            LocalDateTime deadline = LocalDateTime.now().minusHours(1);
            Order order = Order.of(5001L, 1201L, 301L, 302L, BigDecimal.valueOf(85_000),
                    deadline, defaultItemSnapshot());

            // when
            order.cancelByTimeout(LocalDateTime.now());

            // then
            assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        }

        @Test
        @DisplayName("ORDERED 상태에서는 취소할 수 없다")
        void cancelAfterOrdered_throwsException() {
            // given
            Order order = pendingOrder();
            order.confirmOrder(defaultDeliveryInfo(), LocalDateTime.now().plusDays(7), LocalDateTime.now());

            // when & then
            assertThatThrownBy(() -> order.cancelByTimeout(LocalDateTime.now()))
                    .isInstanceOf(OrderException.class)
                    .hasMessage("취소할 수 없는 주문 상태입니다");
        }
    }

    @Nested
    @DisplayName("거래 확정 (completeByBuyer)")
    class CompleteByBuyer {

        @Test
        @DisplayName("ORDERED 상태에서 완료하면 COMPLETED로 바뀐다")
        void completeFromOrdered() {
            // given
            Order order = pendingOrder();
            order.confirmOrder(defaultDeliveryInfo(), LocalDateTime.now().plusDays(7), LocalDateTime.now());

            // when
            LocalDateTime now = LocalDateTime.now();
            order.completeByBuyer(now);

            // then
            assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
            assertThat(order.getCompletedAt()).isEqualTo(now);
        }

        @Test
        @DisplayName("ORDERED가 아니면 예외가 발생한다")
        void completeWhenNotOrdered_throwsException() {
            // given
            Order order = pendingOrder();

            // when & then
            assertThatThrownBy(() -> order.completeByBuyer(LocalDateTime.now()))
                    .isInstanceOf(OrderException.class)
                    .hasMessage("ORDERED 상태의 주문만 거래 확정할 수 있습니다");
        }

        @Test
        @DisplayName("거래 확정 가능 기한이 지나면 예외가 발생한다")
        void completeAfterDeadline_throwsException() {
            // given
            Order order = pendingOrder();
            LocalDateTime completionDeadline = LocalDateTime.now().plusDays(7);
            order.confirmOrder(defaultDeliveryInfo(), completionDeadline, LocalDateTime.now());

            // when & then
            assertThatThrownBy(() -> order.completeByBuyer(completionDeadline.plusSeconds(1)))
                    .isInstanceOf(OrderException.class)
                    .hasMessage("거래 확정 기한이 지났습니다");
        }
    }

    @Nested
    @DisplayName("거래 자동 확정 (completeByTimeout)")
    class CompleteByTimeout {

        @Test
        @DisplayName("데드라인이 지났어도 완료할 수 있다")
        void completeAfterDeadline_succeeds() {
            // given
            Order order = pendingOrder();
            LocalDateTime completionDeadline = LocalDateTime.now().minusMinutes(1);
            order.confirmOrder(defaultDeliveryInfo(), completionDeadline, LocalDateTime.now());

            // when
            order.completeByTimeout(LocalDateTime.now());

            // then
            assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        }

        @Test
        @DisplayName("ORDERED가 아니면 예외가 발생한다")
        void completeWhenNotOrdered_throwsException() {
            // given
            Order order = pendingOrder();

            // when & then
            assertThatThrownBy(() -> order.completeByTimeout(LocalDateTime.now()))
                    .isInstanceOf(OrderException.class)
                    .hasMessage("ORDERED 상태의 주문만 거래 확정할 수 있습니다");
        }
    }

    @Nested
    @DisplayName("환불 신청 (requestRefund)")
    class RequestRefund {

        @Test
        @DisplayName("ORDERED 상태에서 환불을 신청하면 REFUND_REQUESTED로 바뀐다")
        void requestRefundFromOrdered() {
            // given
            Order order = orderedOrder();
            List<String> images = List.of("https://cdn.example.com/refund/1.jpg");
            LocalDateTime now = LocalDateTime.now();

            // when
            order.requestRefund(RefundReason.DEFECTIVE, "박스가 파손되어 도착했습니다.", images, now);

            // then
            assertThat(order.getStatus()).isEqualTo(OrderStatus.REFUND_REQUESTED);
            assertThat(order.getRefundInfo().getReason()).isEqualTo(RefundReason.DEFECTIVE);
            assertThat(order.getRefundInfo().getDescription()).isEqualTo("박스가 파손되어 도착했습니다.");
            assertThat(order.getRefundInfo().getImageUrls()).isEqualTo(images);
            assertThat(order.getRefundInfo().getRequestedAt()).isEqualTo(now);
            assertThat(order.getRefundInfo().getRefundedAt()).isNull();
        }

        @Test
        @DisplayName("설명과 이미지 없이도 환불을 신청할 수 있다")
        void requestRefundWithoutDescriptionAndImages() {
            // given
            Order order = orderedOrder();

            // when
            order.requestRefund(RefundReason.NOT_DELIVERED, null, null, LocalDateTime.now());

            // then
            assertThat(order.getRefundInfo().getDescription()).isNull();
            assertThat(order.getRefundInfo().getImageUrls()).isNull();
        }

        @Test
        @DisplayName("ORDERED가 아니면 예외가 발생한다")
        void requestRefundWhenNotOrdered_throwsException() {
            // given
            Order order = pendingOrder();

            // when & then
            assertThatThrownBy(() -> order.requestRefund(RefundReason.DEFECTIVE, null, null, LocalDateTime.now()))
                    .isInstanceOf(OrderException.class)
                    .hasMessage("ORDERED 상태의 주문만 환불 신청할 수 있습니다");
        }

        @Test
        @DisplayName("환불 신청 가능 기한이 지나면 예외가 발생한다")
        void requestRefundAfterDeadline_throwsException() {
            // given
            Order order = pendingOrder();
            LocalDateTime completionDeadline = LocalDateTime.now().plusDays(7);
            order.confirmOrder(defaultDeliveryInfo(), completionDeadline, LocalDateTime.now());

            // when & then
            assertThatThrownBy(() -> order.requestRefund(
                    RefundReason.DEFECTIVE, null, null, completionDeadline.plusSeconds(1)))
                    .isInstanceOf(OrderException.class)
                    .hasMessage("환불 신청 가능 기한이 지났습니다");
        }
    }

    @Nested
    @DisplayName("환불 승인 (approveRefund)")
    class ApproveRefund {

        @Test
        @DisplayName("REFUND_REQUESTED 상태에서 승인하면 REFUND로 바뀌고 환불 시점이 기록된다")
        void approveFromRefundRequested() {
            // given
            Order order = orderedOrder();
            order.requestRefund(RefundReason.SUSPECTED_FAKE, null, null, LocalDateTime.now());

            // when
            LocalDateTime now = LocalDateTime.now();
            order.approveRefund(now);

            // then
            assertThat(order.getStatus()).isEqualTo(OrderStatus.REFUND);
            assertThat(order.getRefundInfo().getRefundedAt()).isEqualTo(now);
        }

        @Test
        @DisplayName("REFUND_REQUESTED가 아니면 예외가 발생한다")
        void approveWhenNotRequested_throwsException() {
            // given
            Order order = orderedOrder();

            // when & then
            assertThatThrownBy(() -> order.approveRefund(LocalDateTime.now()))
                    .isInstanceOf(OrderException.class)
                    .hasMessage("REFUND_REQUESTED 상태의 주문만 처리할 수 있습니다");
        }
    }

    @Nested
    @DisplayName("환불 반려 (rejectRefund)")
    class RejectRefund {

        @Test
        @DisplayName("REFUND_REQUESTED 상태에서 반려하면 COMPLETED로 바뀐다")
        void rejectFromRefundRequested() {
            // given
            Order order = orderedOrder();
            order.requestRefund(RefundReason.OTHER, null, null, LocalDateTime.now());

            // when
            LocalDateTime now = LocalDateTime.now();
            order.rejectRefund(now);

            // then
            assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
            assertThat(order.getCompletedAt()).isEqualTo(now);
            assertThat(order.getRefundInfo().getRefundedAt()).isNull();
        }

        @Test
        @DisplayName("REFUND_REQUESTED가 아니면 예외가 발생한다")
        void rejectWhenNotRequested_throwsException() {
            // given
            Order order = orderedOrder();

            // when & then
            assertThatThrownBy(() -> order.rejectRefund(LocalDateTime.now()))
                    .isInstanceOf(OrderException.class)
                    .hasMessage("REFUND_REQUESTED 상태의 주문만 처리할 수 있습니다");
        }
    }
}
