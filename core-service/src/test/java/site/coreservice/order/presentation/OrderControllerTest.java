package site.coreservice.order.presentation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import site.coreservice.order.application.OrderService;
import site.coreservice.order.exception.OrderErrorCode;
import site.coreservice.order.exception.OrderException;
import site.coreservice.order.presentation.dto.OrderPlaceRequest;
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

    @Test
    @DisplayName("X-Member-Id 헤더가 없으면 400과 실패 응답을 반환한다")
    void missingMemberIdHeader_isRejected() throws Exception {
        mockMvc.perform(post("/api/v1/orders/{orderId}/cancel", 1L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}
