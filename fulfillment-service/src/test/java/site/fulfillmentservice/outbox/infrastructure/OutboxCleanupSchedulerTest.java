package site.fulfillmentservice.outbox.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import site.fulfillmentservice.outbox.domain.OutboxEventRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("OutboxCleanupScheduler")
class OutboxCleanupSchedulerTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    private OutboxCleanupScheduler sut;

    @BeforeEach
    void setUp() {
        sut = new OutboxCleanupScheduler(outboxEventRepository);
    }

    @Test
    @DisplayName("배치가 꽉 차서 지워지는 동안 계속 반복하고, 꽉 안 차면 멈춘다")
    void deletePublishedEvents_배치가_꽉_차는_동안_반복해서_삭제한다() {
        // given
        when(outboxEventRepository.deletePublishedBefore(any(), eq(500)))
                .thenReturn(500, 500, 120);

        // when
        sut.deletePublishedEvents();

        // then
        verify(outboxEventRepository, times(3)).deletePublishedBefore(any(), eq(500));
    }

    @Test
    @DisplayName("삭제 대상이 없으면 한 번만 실행하고 끝난다")
    void deletePublishedEvents_삭제할_게_없으면_한_번만_실행한다() {
        // given
        when(outboxEventRepository.deletePublishedBefore(any(), eq(500))).thenReturn(0);

        // when
        sut.deletePublishedEvents();

        // then
        verify(outboxEventRepository, times(1)).deletePublishedBefore(any(), eq(500));
    }

    @Test
    @DisplayName("7일 전 시각을 cutoff로 계산해서 넘긴다")
    void deletePublishedEvents_cutoff는_7일_전이다() {
        // given
        when(outboxEventRepository.deletePublishedBefore(any(), anyInt())).thenReturn(0);

        // when
        LocalDateTime before = LocalDateTime.now().minusDays(7);
        sut.deletePublishedEvents();
        LocalDateTime after = LocalDateTime.now().minusDays(7);

        // then
        ArgumentCaptor<LocalDateTime> cutoffCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(outboxEventRepository).deletePublishedBefore(cutoffCaptor.capture(), anyInt());
        assertThat(cutoffCaptor.getValue()).isBetween(before, after);
    }
}
