package site.explorationservice.searchlog.infrastructure;

import lombok.RequiredArgsConstructor;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Repository;
import site.explorationservice.searchlog.domain.SearchClickLog;
import site.explorationservice.searchlog.domain.SearchLog;
import site.explorationservice.searchlog.domain.SearchLogRepository;

/**
 * 문서 타입을 아는 곳은 이 클래스 하나다. 위 계층은 기록 타입만 본다.
 */
@Repository
@RequiredArgsConstructor
public class SearchLogRepositoryImpl implements SearchLogRepository {

    private final ElasticsearchOperations elasticsearchOperations;

    @Override
    public void saveSearchLog(final SearchLog searchLog) {
        elasticsearchOperations.save(SearchLogDocument.from(searchLog));
    }

    @Override
    public void saveClickLog(final SearchClickLog clickLog) {
        elasticsearchOperations.save(SearchClickLogDocument.from(clickLog));
    }
}
