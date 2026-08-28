package site.explorationservice.searchlog.application;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.times;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import site.explorationservice.searchlog.domain.SearchClickLog;
import site.explorationservice.searchlog.domain.SearchLog;
import site.explorationservice.searchlog.domain.SearchLogRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("검색 로그 저장")
class SearchLogServiceTest {

    @Mock
    private SearchLogRepository searchLogRepository;

    @InjectMocks
    private SearchLogService searchLogService;

    @Test
    @DisplayName("검색 기록을 저장 창구로 넘긴다")
    void 검색_기록을_넘긴다() {
        // given
        final SearchLog searchLog = SearchLog.of("0f2c-search-id", "비틀즈", "비틀즈", "NAME",
                0, 20, 42L, 5L, 12L);

        // when
        searchLogService.saveSearchLog(searchLog);

        // then
        then(searchLogRepository).should(times(1)).saveSearchLog(searchLog);
    }

    @Test
    @DisplayName("클릭 기록을 저장 창구로 넘긴다")
    void 클릭_기록을_넘긴다() {
        // given
        final SearchClickLog clickLog = SearchClickLog.of("0f2c-search-id", 12345L, 3);

        // when
        searchLogService.saveClickLog(clickLog);

        // then
        then(searchLogRepository).should(times(1)).saveClickLog(clickLog);
    }

    @Test
    @DisplayName("검색 기록 저장이 실패해도 예외를 밖으로 내보내지 않는다")
    void 검색_기록_저장_실패를_삼킨다() {
        // given
        final SearchLog searchLog = SearchLog.of("0f2c-search-id", "비틀즈", "비틀즈", "NAME",
                0, 20, 42L, 5L, 12L);
        willThrow(new RuntimeException("검색 엔진 응답 없음"))
                .given(searchLogRepository).saveSearchLog(searchLog);

        // when & then
        assertThatCode(() -> searchLogService.saveSearchLog(searchLog)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("클릭 기록 저장이 실패해도 예외를 밖으로 내보내지 않는다")
    void 클릭_기록_저장_실패를_삼킨다() {
        // given
        final SearchClickLog clickLog = SearchClickLog.of("0f2c-search-id", 12345L, 3);
        willThrow(new RuntimeException("검색 엔진 응답 없음"))
                .given(searchLogRepository).saveClickLog(clickLog);

        // when & then
        assertThatCode(() -> searchLogService.saveClickLog(clickLog)).doesNotThrowAnyException();
    }
}
