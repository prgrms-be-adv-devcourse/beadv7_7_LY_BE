package site.fulfillmentservice.order.application;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderCompleteScheduler")
class OrderCompleteSchedulerTest {

    @Mock
    private OrderService orderService;

    @InjectMocks
    private OrderCompleteScheduler orderCompleteScheduler;

    @Test
    @DisplayName("자동 완료 대상 주문 id들을 각각 완료 처리한다")
    void completesEachExpiredOrder() {
        // given
        given(orderService.findOrdersToAutoComplete()).willReturn(List.of(1L, 2L, 3L));

        // when
        orderCompleteScheduler.completeExpiredOrders();

        // then
        verify(orderService).completeExpiredOrder(1L);
        verify(orderService).completeExpiredOrder(2L);
        verify(orderService).completeExpiredOrder(3L);
    }

    @Test
    @DisplayName("한 건 처리에 실패해도 나머지 id는 계속 처리한다")
    void continuesProcessingWhenOneOrderFails() {
        // given
        given(orderService.findOrdersToAutoComplete()).willReturn(List.of(1L, 2L, 3L));
        willThrow(new RuntimeException("처리 실패")).given(orderService).completeExpiredOrder(2L);

        // when
        orderCompleteScheduler.completeExpiredOrders();

        // then
        verify(orderService).completeExpiredOrder(1L);
        verify(orderService).completeExpiredOrder(2L);
        verify(orderService).completeExpiredOrder(3L);
    }
}
