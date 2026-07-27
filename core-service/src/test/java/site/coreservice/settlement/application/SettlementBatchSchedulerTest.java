package site.coreservice.settlement.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("SettlementBatchScheduler")
class SettlementBatchSchedulerTest {

    @Mock
    private SettlementBatchService settlementBatchService;

    @InjectMocks
    private SettlementBatchScheduler settlementBatchScheduler;

    @Captor
    private ArgumentCaptor<LocalDateTime> periodFromCaptor;

    @Captor
    private ArgumentCaptor<LocalDateTime> periodToCaptor;

    @Nested
    @DisplayName("createSettlementBatches")
    class CreateSettlementBatches {

        @Test
        @DisplayName("이번 달 25일을 기준으로 최근 1개월치 기간을 계산해 대상 판매자마다 배치 생성을 요청한다")
        void createsBatchForEachEligibleSeller() {
            // given
            LocalDateTime expectedPeriodTo = LocalDate.now().withDayOfMonth(25).atStartOfDay();
            given(settlementBatchService.findEligibleSellerIds(expectedPeriodTo))
                    .willReturn(List.of(301L, 302L));

            // when
            settlementBatchScheduler.createSettlementBatches();

            // then
            verify(settlementBatchService).createBatchForSeller(
                    eq(301L), periodFromCaptor.capture(), periodToCaptor.capture(), any());
            verify(settlementBatchService).createBatchForSeller(
                    eq(302L), any(), any(), any());
            assertThat(periodToCaptor.getValue()).isEqualTo(expectedPeriodTo);
            assertThat(periodFromCaptor.getValue()).isEqualTo(expectedPeriodTo.minusMonths(1));
        }

        @Test
        @DisplayName("한 판매자 처리 중 예외가 발생해도 나머지 판매자 처리는 계속된다")
        void isolatesFailurePerSeller() {
            // given
            LocalDateTime expectedPeriodTo = LocalDate.now().withDayOfMonth(25).atStartOfDay();
            given(settlementBatchService.findEligibleSellerIds(expectedPeriodTo))
                    .willReturn(List.of(301L, 302L));
            willThrow(new IllegalStateException("boom"))
                    .given(settlementBatchService).createBatchForSeller(eq(301L), any(), any(), any());

            // when
            settlementBatchScheduler.createSettlementBatches();

            // then
            verify(settlementBatchService).createBatchForSeller(eq(302L), any(), any(), any());
        }

        @Test
        @DisplayName("대상 판매자가 없으면 배치 생성을 요청하지 않는다")
        void doesNothingWhenNoEligibleSellers() {
            // given
            LocalDateTime expectedPeriodTo = LocalDate.now().withDayOfMonth(25).atStartOfDay();
            given(settlementBatchService.findEligibleSellerIds(expectedPeriodTo))
                    .willReturn(List.of());

            // when
            settlementBatchScheduler.createSettlementBatches();

            // then
            verify(settlementBatchService, never()).createBatchForSeller(any(), any(), any(), any());
        }
    }
}
