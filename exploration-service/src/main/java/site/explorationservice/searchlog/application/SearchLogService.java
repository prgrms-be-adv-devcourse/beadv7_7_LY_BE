package site.explorationservice.searchlog.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import site.explorationservice.searchlog.application.dto.SearchLogCommand;
import site.explorationservice.searchlog.domain.SearchClickLog;
import site.explorationservice.searchlog.domain.SearchLog;
import site.explorationservice.searchlog.domain.SearchLogRepository;

/**
 * 기록을 전용 스레드 풀에서 저장한다.
 * <p>
 * 트랜잭션 애너테이션을 붙이지 않는다. 이 서비스는 데이터베이스를 쓰지 않는다.
 * <p>
 * <b>다른 빈에서 불러야 비동기가 걸린다.</b> 비동기 표시는 스프링이 이 객체를 대신 받아주는 껍데기를 씌워서
 * 동작하는데, 같은 클래스 안에서 자기 메서드를 부르면 그 껍데기를 거치지 않아 그냥 순서대로 실행된다.
 * <p>
 * 저장 실패를 여기서 삼킨다. 기록이 남지 않는 것보다 검색이 실패하는 쪽이 훨씬 나쁘다. 다만 아무 흔적 없이
 * 사라지면 원인을 찾을 수 없으므로 경고를 남긴다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchLogService {

    private final SearchLogRepository searchLogRepository;

    @Async("searchLogExecutor")
    public void saveSearchLog(final SearchLogCommand command) {
        try {
            searchLogRepository.saveSearchLog(SearchLog.of(command.searchId(), command.keyword(),
                    command.normalizedKeyword(), command.searchBy(), command.page(), command.size(),
                    command.resultCount(), command.engineMillis(), command.elapsedMillis()));
        } catch (final Exception e) {
            log.warn("검색 기록을 저장하지 못했습니다 — 검색 식별자 {}", command.searchId(), e);
        }
    }

    @Async("searchLogExecutor")
    public void saveClickLog(final SearchClickLog clickLog) {
        try {
            searchLogRepository.saveClickLog(clickLog);
        } catch (final Exception e) {
            log.warn("클릭 기록을 저장하지 못했습니다 — 검색 식별자 {}", clickLog.searchId(), e);
        }
    }
}
