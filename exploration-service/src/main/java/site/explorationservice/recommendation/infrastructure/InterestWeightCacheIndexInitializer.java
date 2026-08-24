package site.explorationservice.recommendation.infrastructure;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.stereotype.Component;

/**
 * 로컬에서 회원 가중치 캐시 인덱스가 없으면 매핑과 함께 만들어 두고, 있으면 매핑에 새로 추가된 필드만 반영한다.
 */
@Slf4j
@Component
@Profile("local")
@RequiredArgsConstructor
public class InterestWeightCacheIndexInitializer implements ApplicationRunner {

    private final ElasticsearchOperations elasticsearchOperations;

    @Override
    public void run(final ApplicationArguments args) {
        final IndexOperations indexOperations =
            elasticsearchOperations.indexOps(MemberInterestWeightsDocument.class);
        final String indexName = indexOperations.getIndexCoordinates().getIndexName();

        if (indexOperations.exists()) {
            indexOperations.putMapping();
            log.info("회원 가중치 캐시 인덱스가 이미 있어 매핑에 새 필드만 반영했습니다 — {}", indexName);
            return;
        }

        indexOperations.createWithMapping();
        log.info("회원 가중치 캐시 인덱스를 생성했습니다 — {}", indexName);
    }
}
