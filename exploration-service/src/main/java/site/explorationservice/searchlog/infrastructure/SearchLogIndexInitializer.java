package site.explorationservice.searchlog.infrastructure;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.stereotype.Component;

/**
 * 로그 인덱스가 없으면 매핑과 함께 만든다.
 * <p>
 * <b>인덱스가 없으면 조용히 잘못된 매핑이 생긴다.</b> 인덱스 없이 문서를 저장하면 검색 엔진이 알아서 인덱스를
 * 만드는데, 그때는 값을 보고 타입을 추측한다. 시각이 문자열로 잡히면 시계열 그래프를 그릴 수 없고, 알아채는
 * 시점에는 인덱스를 지우고 다시 만드는 것 말고 방법이 없다.
 * <p>
 * 실패해도 애플리케이션 기동을 막지 않는다. 인덱스를 만들지 못했다고 검색까지 멈출 이유가 없다.
 * <p>
 * 상품 인덱스와 달리 별칭을 쓰지 않는다. 별칭은 매핑을 바꿀 때 기존 문서를 새 인덱스로 옮기기 위한 장치인데,
 * 이 기록은 옮길 만한 가치가 없어서 매핑을 바꿔야 하면 새 인덱스를 만들고 옛것은 두면 된다.
 */
@Slf4j
@Component
@Profile("local")
@RequiredArgsConstructor
public class SearchLogIndexInitializer implements ApplicationRunner {

    private final ElasticsearchOperations elasticsearchOperations;

    @Override
    public void run(final ApplicationArguments args) {
        createIfAbsent(SearchLogDocument.class, "search_logs");
        createIfAbsent(SearchClickLogDocument.class, "search_click_logs");
    }

    private void createIfAbsent(final Class<?> documentType, final String indexName) {
        try {
            final IndexOperations indexOperations = elasticsearchOperations.indexOps(documentType);
            if (indexOperations.exists()) {
                indexOperations.putMapping();
                return;
            }
            indexOperations.createWithMapping();
            log.info("검색 로그 인덱스를 만들었습니다 — {}", indexName);
        } catch (final Exception e) {
            log.warn("검색 로그 인덱스를 만들지 못했습니다 — {}. 기록이 쌓이지 않으므로 원인을 확인해야 합니다",
                indexName, e);
        }
    }
}
