package site.coreservice.order.application;

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
@DisplayName("OrderCancelScheduler")
class OrderCancelSchedulerTest {

    @Mock
    private OrderService orderService;

    @InjectMocks
    private OrderCancelScheduler orderCancelScheduler;

    @Test
    @DisplayName("만료된 주문 id들을 각각 취소 처리한다")
    void cancelsEachExpiredOrder() {
        // given
        given(orderService.findExpiredOrderIds()).willReturn(List.of(1L, 2L, 3L));

        // when
        orderCancelScheduler.cancelExpiredOrders();

        // then
        verify(orderService).cancelExpiredOrder(1L);
        verify(orderService).cancelExpiredOrder(2L);
        verify(orderService).cancelExpiredOrder(3L);
    }

    @Test
    @DisplayName("한 건 처리에 실패해도 나머지 id는 계속 처리한다")
    void continuesProcessingWhenOneOrderFails() {
        // given
        given(orderService.findExpiredOrderIds()).willReturn(List.of(1L, 2L, 3L));
        willThrow(new RuntimeException("처리 실패")).given(orderService).cancelExpiredOrder(2L);

        // when
        orderCancelScheduler.cancelExpiredOrders();

        // then
        verify(orderService).cancelExpiredOrder(1L);
        verify(orderService).cancelExpiredOrder(2L);
        verify(orderService).cancelExpiredOrder(3L);
    }
}
