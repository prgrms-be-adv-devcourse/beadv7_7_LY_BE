package site.fulfillmentservice.order.presentation;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import site.fulfillmentservice.order.application.OrderService;
import site.fulfillmentservice.order.application.dto.OrderDetailResult;
import site.fulfillmentservice.order.application.dto.OrderItemSnapshotResult;
import site.fulfillmentservice.order.application.dto.OrderSearchResult;
import site.fulfillmentservice.order.application.dto.OrderSummaryResult;
import site.fulfillmentservice.order.exception.OrderErrorCode;
import site.fulfillmentservice.order.exception.OrderException;

@WebMvcTest(OrderAdminController.class)
@DisplayName("OrderAdminController")
class OrderAdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    @Nested
    @DisplayName("GET /api/admin/v1/orders")
    class GetOrders {

        private final OrderItemSnapshotResult product = new OrderItemSnapshotResult(
                1201L, "Abbey Road", "비틀즈", 1969, "ORIGINAL", "VERY_GOOD_PLUS",
                "https://cdn.example.com/listings/5001/photo1.jpg");

        @Test
        @DisplayName("성공하면 200과 주문 목록을 반환한다")
        void getOrders_success() throws Exception {
            OrderSummaryResult summary = new OrderSummaryResult(
                    1L, 5001L, "REFUND_REQUESTED", BigDecimal.valueOf(85_000),
                    LocalDateTime.now().plusHours(24), LocalDateTime.now().plusDays(7), product);
            OrderSearchResult result = new OrderSearchResult(List.of(summary), 0, 20, 1L, 1, true);
            given(orderService.findOrdersForAdmin("REFUND_REQUESTED", 0, 20)).willReturn(result);

            mockMvc.perform(get("/api/admin/v1/orders")
                            .param("status", "REFUND_REQUESTED"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content[0].orderId").value(1))
                    .andExpect(jsonPath("$.data.content[0].status").value("REFUND_REQUESTED"));
        }

        @Test
        @DisplayName("status 문자열이 유효하지 않으면 400과 실패 응답을 반환한다")
        void getOrders_invalidStatus() throws Exception {
            given(orderService.findOrdersForAdmin("NOT_A_STATUS", 0, 20))
                    .willThrow(new OrderException(OrderErrorCode.INVALID_STATUS));

            mockMvc.perform(get("/api/admin/v1/orders")
                            .param("status", "NOT_A_STATUS"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("OERR-2009"));
        }
    }

    @Nested
    @DisplayName("GET /api/admin/v1/orders/{orderId}")
    class GetOrder {

        private final OrderItemSnapshotResult product = new OrderItemSnapshotResult(
                1201L, "Abbey Road", "비틀즈", 1969, "ORIGINAL", "VERY_GOOD_PLUS",
                "https://cdn.example.com/listings/5001/photo1.jpg");

        @Test
        @DisplayName("성공하면 200과 주문 상세 정보를 반환한다")
        void getOrder_success() throws Exception {
            OrderDetailResult result = new OrderDetailResult(
                    1L, 5001L, 301L, 302L, BigDecimal.valueOf(85_000), "ORDERED", null,
                    LocalDateTime.now().plusHours(24), LocalDateTime.now().plusDays(7),
                    LocalDateTime.now(), null, null, product, null, null);
            given(orderService.getOrderDetailForAdmin(1L)).willReturn(result);

            mockMvc.perform(get("/api/admin/v1/orders/{orderId}", 1L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.orderId").value(1))
                    .andExpect(jsonPath("$.data.status").value("ORDERED"));
        }

        @Test
        @DisplayName("존재하지 않는 주문이면 404와 실패 응답을 반환한다")
        void getOrder_orderNotFound() throws Exception {
            given(orderService.getOrderDetailForAdmin(1L))
                    .willThrow(new OrderException(OrderErrorCode.ORDER_NOT_FOUND));

            mockMvc.perform(get("/api/admin/v1/orders/{orderId}", 1L))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("OERR-2001"));
        }
    }

    @Nested
    @DisplayName("POST /api/admin/v1/orders/{orderId}/refund-approve")
    class ApproveRefund {

        @Test
        @DisplayName("성공하면 200과 성공 응답을 반환한다")
        void approveRefund_success() throws Exception {
            mockMvc.perform(post("/api/admin/v1/orders/{orderId}/refund-approve", 1L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(orderService).approveRefund(1L);
        }

        @Test
        @DisplayName("REFUND_REQUESTED 상태가 아니면 409와 실패 응답을 반환한다")
        void approveRefund_notRequested() throws Exception {
            willThrow(new OrderException(OrderErrorCode.REFUND_NOT_REQUESTED))
                    .given(orderService).approveRefund(1L);

            mockMvc.perform(post("/api/admin/v1/orders/{orderId}/refund-approve", 1L))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("OERR-2011"));
        }
    }

    @Nested
    @DisplayName("POST /api/admin/v1/orders/{orderId}/refund-reject")
    class RejectRefund {

        @Test
        @DisplayName("성공하면 200과 성공 응답을 반환한다")
        void rejectRefund_success() throws Exception {
            mockMvc.perform(post("/api/admin/v1/orders/{orderId}/refund-reject", 1L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(orderService).rejectRefund(1L);
        }

        @Test
        @DisplayName("REFUND_REQUESTED 상태가 아니면 409와 실패 응답을 반환한다")
        void rejectRefund_notRequested() throws Exception {
            willThrow(new OrderException(OrderErrorCode.REFUND_NOT_REQUESTED))
                    .given(orderService).rejectRefund(1L);

            mockMvc.perform(post("/api/admin/v1/orders/{orderId}/refund-reject", 1L))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("OERR-2011"));
        }
    }
}
