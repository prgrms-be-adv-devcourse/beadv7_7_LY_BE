package site.fulfillmentservice.order.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import site.fulfillmentservice.order.application.OrderService;
import site.fulfillmentservice.order.application.dto.DeliveryAddressResult;
import site.fulfillmentservice.order.application.dto.OrderDetailResult;
import site.fulfillmentservice.order.application.dto.OrderSearchResult;
import site.fulfillmentservice.order.application.dto.OrderSummaryResult;
import site.fulfillmentservice.order.application.dto.OrderItemSnapshotResult;
import site.fulfillmentservice.order.application.dto.RefundInfoResult;
import site.fulfillmentservice.order.domain.RefundReason;
import site.fulfillmentservice.order.exception.OrderErrorCode;
import site.fulfillmentservice.order.exception.OrderException;
import site.fulfillmentservice.order.presentation.dto.OrderPlaceRequest;
import site.fulfillmentservice.order.presentation.dto.RefundRequest;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(OrderController.class)
@DisplayName("OrderController")
class OrderControllerTest {

    private static final String MEMBER_ID_HEADER = "X-Member-Id";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrderService orderService;

    @Nested
    @DisplayName("POST /api/v1/orders/{orderId}/place")
    class PlaceOrder {

        private final OrderPlaceRequest request =
                new OrderPlaceRequest("홍길동", "010-1234-5678", "서울시 강남구", "101동 202호");

        @Test
        @DisplayName("성공하면 200과 성공 응답을 반환한다")
        void placeOrder_success() throws Exception {
            mockMvc.perform(post("/api/v1/orders/{orderId}/place", 1L)
                            .header(MEMBER_ID_HEADER, "301")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(orderService).placeOrder(1L, 301L, request.toDeliveryInfo());
        }

        @Test
        @DisplayName("주문을 찾을 수 없으면 404와 실패 응답을 반환한다")
        void placeOrder_orderNotFound() throws Exception {
            willThrow(new OrderException(OrderErrorCode.ORDER_NOT_FOUND))
                    .given(orderService).placeOrder(any(), any(), any());

            mockMvc.perform(post("/api/v1/orders/{orderId}/place", 1L)
                            .header(MEMBER_ID_HEADER, "301")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("OERR-2001"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/orders/{orderId}/cancel")
    class CancelOrder {

        @Test
        @DisplayName("성공하면 200과 성공 응답을 반환한다")
        void cancelOrder_success() throws Exception {
            mockMvc.perform(post("/api/v1/orders/{orderId}/cancel", 1L)
                            .header(MEMBER_ID_HEADER, "301"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(orderService).cancelOrder(1L, 301L);
        }

        @Test
        @DisplayName("본인 주문이 아니면 403과 실패 응답을 반환한다")
        void cancelOrder_accessDenied() throws Exception {
            willThrow(new OrderException(OrderErrorCode.ORDER_ACCESS_DENIED))
                    .given(orderService).cancelOrder(1L, 999L);

            mockMvc.perform(post("/api/v1/orders/{orderId}/cancel", 1L)
                            .header(MEMBER_ID_HEADER, "999"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("OERR-2002"));
        }

        @Test
        @DisplayName("PENDING이 아니면 409와 실패 응답을 반환한다")
        void cancelOrder_notCancellable() throws Exception {
            willThrow(new OrderException(OrderErrorCode.ORDER_NOT_CANCELLABLE))
                    .given(orderService).cancelOrder(1L, 301L);

            mockMvc.perform(post("/api/v1/orders/{orderId}/cancel", 1L)
                            .header(MEMBER_ID_HEADER, "301"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("OERR-2006"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/orders/{orderId}/complete")
    class CompleteOrder {

        @Test
        @DisplayName("성공하면 200과 성공 응답을 반환한다")
        void completeOrder_success() throws Exception {
            mockMvc.perform(post("/api/v1/orders/{orderId}/complete", 1L)
                            .header(MEMBER_ID_HEADER, "301"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(orderService).completeOrder(1L, 301L);
        }

        @Test
        @DisplayName("ORDERED 상태가 아니면 409와 실패 응답을 반환한다")
        void completeOrder_notOrdered() throws Exception {
            willThrow(new OrderException(OrderErrorCode.ORDER_NOT_ORDERED))
                    .given(orderService).completeOrder(1L, 301L);

            mockMvc.perform(post("/api/v1/orders/{orderId}/complete", 1L)
                            .header(MEMBER_ID_HEADER, "301"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("OERR-2007"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/orders/{orderId}/refund-request")
    class RequestRefund {

        private final RefundRequest request =
                new RefundRequest(RefundReason.DEFECTIVE, "박스가 파손되어 도착했습니다.",
                        List.of("https://cdn.example.com/refund/1.jpg"));

        @Test
        @DisplayName("성공하면 200과 성공 응답을 반환한다")
        void requestRefund_success() throws Exception {
            mockMvc.perform(post("/api/v1/orders/{orderId}/refund-request", 1L)
                            .header(MEMBER_ID_HEADER, "301")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(orderService).requestRefund(1L, 301L, request.toCommand());
        }

        @Test
        @DisplayName("ORDERED 상태가 아니면 409와 실패 응답을 반환한다")
        void requestRefund_notRefundable() throws Exception {
            willThrow(new OrderException(OrderErrorCode.ORDER_NOT_REFUNDABLE))
                    .given(orderService).requestRefund(any(), any(), any());

            mockMvc.perform(post("/api/v1/orders/{orderId}/refund-request", 1L)
                            .header(MEMBER_ID_HEADER, "301")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("OERR-2010"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/orders/{orderId}")
    class GetOrder {

        private final OrderItemSnapshotResult product = new OrderItemSnapshotResult(
                1201L, "Abbey Road", "비틀즈", 1969, "ORIGINAL", "VERY_GOOD_PLUS",
                "https://cdn.example.com/listings/5001/photo1.jpg");

        private final DeliveryAddressResult deliveryAddress =
                new DeliveryAddressResult("홍길동", "010-1234-5678", "서울시 강남구", "101동 202호");

        @Test
        @DisplayName("성공하면 200과 주문 상세 정보를 반환한다")
        void getOrder_success() throws Exception {
            OrderDetailResult result = new OrderDetailResult(
                    1L, 5001L, 301L, 302L, BigDecimal.valueOf(85_000), "ORDERED", null,
                    LocalDateTime.now().plusHours(24), LocalDateTime.now().plusDays(7),
                    LocalDateTime.now(), null, null, product, deliveryAddress, null);
            given(orderService.getOrderDetail(1L, 301L)).willReturn(result);

            mockMvc.perform(get("/api/v1/orders/{orderId}", 1L)
                            .header(MEMBER_ID_HEADER, "301"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.orderId").value(1))
                    .andExpect(jsonPath("$.data.status").value("ORDERED"))
                    .andExpect(jsonPath("$.data.product.productId").value(1201))
                    .andExpect(jsonPath("$.data.product.albumTitle").value("Abbey Road"))
                    .andExpect(jsonPath("$.data.deliveryAddress.recipientName").value("홍길동"))
                    .andExpect(jsonPath("$.data.refundInfo").doesNotExist());
        }

        @Test
        @DisplayName("환불 신청 중이면 환불 정보를 함께 반환한다")
        void getOrder_withRefundInfo() throws Exception {
            RefundInfoResult refundInfo = new RefundInfoResult(
                    "DEFECTIVE", "박스가 파손되어 도착했습니다",
                    List.of("https://cdn.example.com/refund/1.jpg"),
                    LocalDateTime.now(), null);
            OrderDetailResult result = new OrderDetailResult(
                    1L, 5001L, 301L, 302L, BigDecimal.valueOf(85_000), "REFUND_REQUESTED", null,
                    LocalDateTime.now().plusHours(24), LocalDateTime.now().plusDays(7),
                    LocalDateTime.now(), null, null, product, deliveryAddress, refundInfo);
            given(orderService.getOrderDetail(1L, 301L)).willReturn(result);

            mockMvc.perform(get("/api/v1/orders/{orderId}", 1L)
                            .header(MEMBER_ID_HEADER, "301"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("REFUND_REQUESTED"))
                    .andExpect(jsonPath("$.data.refundInfo.reason").value("DEFECTIVE"))
                    .andExpect(jsonPath("$.data.refundInfo.description").value("박스가 파손되어 도착했습니다"))
                    .andExpect(jsonPath("$.data.refundInfo.refundedAt").doesNotExist());
        }

        @Test
        @DisplayName("주문을 찾을 수 없으면 404와 실패 응답을 반환한다")
        void getOrder_orderNotFound() throws Exception {
            given(orderService.getOrderDetail(1L, 301L))
                    .willThrow(new OrderException(OrderErrorCode.ORDER_NOT_FOUND));

            mockMvc.perform(get("/api/v1/orders/{orderId}", 1L)
                            .header(MEMBER_ID_HEADER, "301"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("OERR-2001"));
        }

        @Test
        @DisplayName("구매자/판매자가 아니면 403과 실패 응답을 반환한다")
        void getOrder_accessDenied() throws Exception {
            given(orderService.getOrderDetail(1L, 999L))
                    .willThrow(new OrderException(OrderErrorCode.ORDER_ACCESS_DENIED));

            mockMvc.perform(get("/api/v1/orders/{orderId}", 1L)
                            .header(MEMBER_ID_HEADER, "999"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("OERR-2002"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/orders")
    class GetOrders {

        @Test
        @DisplayName("성공하면 200과 주문 목록 페이지를 반환한다")
        void getOrders_success() throws Exception {
            OrderItemSnapshotResult product = new OrderItemSnapshotResult(
                    1201L, "Abbey Road", "비틀즈", 1969, "ORIGINAL", "VERY_GOOD_PLUS",
                    "https://cdn.example.com/listings/5001/photo1.jpg");
            OrderSummaryResult summary = new OrderSummaryResult(
                    1L, 5001L, "ORDERED", BigDecimal.valueOf(85_000),
                    LocalDateTime.now().plusHours(24), LocalDateTime.now().plusDays(7), product);
            OrderSearchResult searchResult = new OrderSearchResult(List.of(summary), 0, 20, 1L, 1, true);
            given(orderService.findOrders(301L, "buyer", "ORDERED", 0, 20)).willReturn(searchResult);

            mockMvc.perform(get("/api/v1/orders")
                            .header(MEMBER_ID_HEADER, "301")
                            .param("perspective", "buyer")
                            .param("status", "ORDERED"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content[0].orderId").value(1))
                    .andExpect(jsonPath("$.data.content[0].product.productId").value(1201))
                    .andExpect(jsonPath("$.data.totalElements").value(1))
                    .andExpect(jsonPath("$.data.last").value(true));
        }

        @Test
        @DisplayName("perspective가 유효하지 않으면 400과 실패 응답을 반환한다")
        void getOrders_invalidPerspective() throws Exception {
            given(orderService.findOrders(301L, "invalid", null, 0, 20))
                    .willThrow(new OrderException(OrderErrorCode.INVALID_PERSPECTIVE));

            mockMvc.perform(get("/api/v1/orders")
                            .header(MEMBER_ID_HEADER, "301")
                            .param("perspective", "invalid"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("OERR-2008"));
        }

        @Test
        @DisplayName("status가 유효하지 않으면 400과 실패 응답을 반환한다")
        void getOrders_invalidStatus() throws Exception {
            given(orderService.findOrders(301L, "buyer", "INVALID", 0, 20))
                    .willThrow(new OrderException(OrderErrorCode.INVALID_STATUS));

            mockMvc.perform(get("/api/v1/orders")
                            .header(MEMBER_ID_HEADER, "301")
                            .param("perspective", "buyer")
                            .param("status", "INVALID"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("OERR-2009"));
        }
    }

    @Test
    @DisplayName("X-Member-Id 헤더가 없으면 400과 실패 응답을 반환한다")
    void missingMemberIdHeader_isRejected() throws Exception {
        mockMvc.perform(post("/api/v1/orders/{orderId}/cancel", 1L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}
