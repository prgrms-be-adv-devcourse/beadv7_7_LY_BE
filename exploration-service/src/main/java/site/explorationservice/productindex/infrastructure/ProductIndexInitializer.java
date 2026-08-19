package site.explorationservice.productindex.infrastructure;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ApplicationArguments;
import org.springframework.context.annotation.Profile;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.stereotype.Component;
import site.explorationservice.productindex.domain.ProductDocument;

/**
 * 로컬에서 상품 인덱스가 없으면 매핑과 함께 만들어 두고, 있으면 매핑에 새로 추가된 필드만 반영한다.
 * <p>
 * <b>인덱스가 없으면 조용히 깨진다.</b> 인덱스가 없는 상태로 문서를 저장하면 ES가 dynamic mapping으로 인덱스를
 * 자동 생성하는데, 그러면 contentVector가 dense_vector가 아니라 단순 float 배열로 잡힌다. 색인은 성공하지만 kNN 검색이 안 되고, 알아채는
 * 시점에는 이미 재색인 말고는 고칠 방법이 없다.
 * <p>
 * <b>이미 있으면 put-mapping으로 병합한다(교체가 아니다).</b> ES의 put-mapping API는 새 필드만 추가하고, 기존 필드의
 * analyzer·dims·similarity를 바꾸려는 시도는 ES가 타입 충돌로 거부한다 — 그래서 JPA의 {@code ddl-auto: update}처럼 "필드 추가는
 * 자동, 기존 필드 변경은 불가"인 동작이 안전하게 성립한다. MySQL에 {@code ddl-auto: none}을 쓰는 이유(같은 스키마를 core-service·
 * member-service 두 앱이 공유해서 생기는 교차 서비스 사고)는 이 인덱스엔 해당하지 않는다 — 검색·추천이 한 모듈로 합쳐지면서 이 인덱스를 쓰는 앱은
 * exploration-service 하나뿐이다.
 * <p>
 * 로컬 프로파일에만 두는 건 다른 이유다. <b>운영에서 인덱스를 누가 만들고 매핑을 누가 갱신할지는 아직 정하지 않았다</b> — 배포 절차의 일부로 할지, 별도 마이그레이션
 * 도구를 둘지는 결정이 필요하다. 그때까지 운영 환경은 인덱스가 미리 준비돼 있어야 한다.
 */
@Slf4j
@Component
@Profile("local")
@RequiredArgsConstructor
public class ProductIndexInitializer implements ApplicationRunner {

    private final ElasticsearchOperations elasticsearchOperations;

    @Override
    public void run(final ApplicationArguments args) {
        final IndexOperations indexOperations =
            elasticsearchOperations.indexOps(ProductDocument.class);
        final String indexName = indexOperations.getIndexCoordinates().getIndexName();

        if (indexOperations.exists()) {
            indexOperations.putMapping();
            log.info("상품 인덱스가 이미 있어 매핑에 새 필드만 반영했습니다 — {}", indexName);
            return;
        }

        indexOperations.createWithMapping();
        log.info("상품 인덱스를 생성했습니다 — {}", indexName);
    }
}
